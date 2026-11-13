package org.f7.motoactions.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import org.f7.motoactions.BuildConfig
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val zipUrl: String,
    val changelogUrl: String,
    val source: String,
    val sha256: String? = null,
)

sealed class UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult()

    data class UpToDate(val currentCode: Int, val remoteCode: Int) : UpdateCheckResult()

    data class Error(val message: String) : UpdateCheckResult()
}

sealed class RootStatus {
    data object Granted : RootStatus()

    data object Denied : RootStatus()

    data object Unavailable : RootStatus()
}

object ModuleUpdater {
    const val GITHUB_OWNER = "GustavoCaruzoGoncalves"
    const val GITHUB_REPO = "onyx-gestures"
    const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/module/update.json"
    const val GITHUB_LATEST_API =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val RELEASES_PAGE =
        "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    /** Kept for KernelSU / installed-module compatibility. */
    const val MODULE_ID = "f7_moto_actions"

    private val KSU_PACKAGES =
        listOf(
            "me.weishu.kernelsu",
            "com.rifsxd.ksunext",
            "com.android.system.ksu",
        )

    fun localVersionCode(): Int {
        val fromModule =
            runCatching {
                val out =
                    suCapture(
                        "grep '^versionCode=' /data/adb/modules/$MODULE_ID/module.prop 2>/dev/null | head -1",
                    )
                out.lineSequence()
                    .mapNotNull { line ->
                        line.substringAfter("=", "").trim().toIntOrNull()
                    }.firstOrNull()
            }.getOrNull()
        return fromModule ?: BuildConfig.VERSION_CODE
    }

    fun checkForUpdate(): UpdateCheckResult {
        val localCode = BuildConfig.VERSION_CODE
        val localName = BuildConfig.VERSION_NAME.removePrefix("v")
        return runCatching {
            val info = fetchUpdateInfo()
            val remoteName = info.version.removePrefix("v")
            val newerByCode = info.versionCode > localCode
            val newerByName =
                info.source == "github-api" &&
                    remoteName.isNotBlank() &&
                    !remoteName.equals(localName, ignoreCase = true)
            if (newerByCode || newerByName) {
                UpdateCheckResult.Available(info)
            } else {
                UpdateCheckResult.UpToDate(localCode, info.versionCode)
            }
        }.getOrElse { UpdateCheckResult.Error(it.message ?: it.toString()) }
    }

    fun fetchUpdateInfo(): UpdateInfo {
        runCatching { fetchFromUpdateJson() }.getOrNull()?.let { return it }
        return fetchFromGithubApi()
    }

    private fun fetchFromUpdateJson(): UpdateInfo {
        val body = httpGet(UPDATE_JSON_URL)
        val o = JSONObject(body)
        val version = o.getString("version")
        val code = o.getInt("versionCode")
        val zip = o.getString("zipUrl")
        val changelog = o.optString("changelog", RELEASES_PAGE)
        val sha = o.optString("sha256", "").ifBlank { null }
        require(zip.isNotBlank()) { "update.json missing zipUrl" }
        return UpdateInfo(version, code, zip, changelog, "update.json", sha)
    }

    private fun fetchFromGithubApi(): UpdateInfo {
        val body = httpGet(GITHUB_LATEST_API)
        val o = JSONObject(body)
        val tag = o.optString("tag_name", o.optString("name", "latest"))
        val assets = o.getJSONArray("assets")
        var zipUrl = ""
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            if (name.endsWith(".zip") &&
                (
                    name.contains("f7_moto_actions", ignoreCase = true) ||
                        name.contains("onyx_gestures", ignoreCase = true)
                )
            ) {
                zipUrl = a.getString("browser_download_url")
                break
            }
        }
        require(zipUrl.isNotBlank()) { "No module ZIP in latest GitHub release" }
        val remoteName = tag.removePrefix("v")
        val localName = BuildConfig.VERSION_NAME.removePrefix("v")
        val code =
            if (remoteName.equals(localName, ignoreCase = true)) {
                BuildConfig.VERSION_CODE
            } else {
                BuildConfig.VERSION_CODE + 1
            }
        return UpdateInfo(tag, code, zipUrl, RELEASES_PAGE, "github-api")
    }

    fun downloadZip(
        context: Context,
        url: String,
        expectedSha256: String? = null,
        onProgress: ((Long, Long) -> Unit)? = null,
    ): File {
        val out = File(context.cacheDir, "onyx_gestures_update.zip")
        if (out.exists()) out.delete()
        val conn =
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 120_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "OnyxGestures/${BuildConfig.VERSION_NAME}")
            }
        try {
            if (conn.responseCode !in 200..299) {
                error("Download HTTP ${conn.responseCode}")
            }
            val total = conn.contentLengthLong
            val digest = MessageDigest.getInstance("SHA-256")
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(out).use { output ->
                    val buf = ByteArray(64 * 1024)
                    var readTotal = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        readTotal += n
                        onProgress?.invoke(readTotal, total)
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!expectedSha256.isNullOrBlank() &&
                !expectedSha256.equals(actual, ignoreCase = true)
            ) {
                out.delete()
                error("SHA-256 mismatch (expected $expectedSha256, got $actual)")
            }
        } finally {
            conn.disconnect()
        }
        require(out.length() > 1024) { "Downloaded ZIP too small" }
        return out
    }

    /**
     * Probe root via standard `su`. Does not write KernelSU policy files.
     * KernelSU will prompt / deny based on Manager allowlist.
     */
    fun probeRoot(): RootStatus {
        if (!suBinaryPresent()) return RootStatus.Unavailable
        return runCatching {
            val result = suCaptureDetailed("id")
            when {
                result.output.contains("uid=0") -> RootStatus.Granted
                result.exitCode != 0 -> RootStatus.Denied
                else -> RootStatus.Denied
            }
        }.getOrElse { RootStatus.Denied }
    }

    fun installWithRoot(zip: File): String {
        require(zip.exists()) { "ZIP missing" }
        when (val root = probeRoot()) {
            RootStatus.Unavailable ->
                error("ROOT_UNAVAILABLE")
            RootStatus.Denied ->
                error("ROOT_DENIED")
            RootStatus.Granted -> Unit
        }
        val ksud =
            listOf(
                "/data/adb/ksu/bin/ksud",
                "/data/adb/ksud",
            ).firstOrNull { path ->
                suCapture("[ -x '$path' ] && echo ok").contains("ok")
            } ?: error("ROOT_UNAVAILABLE")

        val staged = "/data/local/tmp/onyx_gestures_update.zip"
        suCapture("cp '${zip.absolutePath}' '$staged' && chmod 644 '$staged'")
        val log = suCapture("'$ksud' module install '$staged' 2>&1")
        suCapture("rm -f '$staged'")
        val ok =
            log.contains("Module installed successfully", ignoreCase = true) ||
                (log.contains("Installed", ignoreCase = true) && log.contains(MODULE_ID))
        if (!ok) {
            if (log.contains("error", ignoreCase = true) || log.contains("fail", ignoreCase = true)) {
                error(log.take(800).ifBlank { "Module install failed" })
            }
            error("Install not confirmed by ksud:\n${log.take(800)}")
        }
        // Confirm module.prop landed in modules_update or modules
        val prop =
            suCapture(
                "cat /data/adb/modules_update/$MODULE_ID/module.prop 2>/dev/null || " +
                    "cat /data/adb/modules/$MODULE_ID/module.prop 2>/dev/null",
            )
        require(prop.contains("versionCode=")) { "Install not confirmed: module.prop missing" }
        return log
    }

    fun rebootDevice() {
        when (probeRoot()) {
            RootStatus.Granted -> suCapture("reboot")
            else -> error("ROOT_DENIED")
        }
    }

    /**
     * Opens the installed KernelSU Manager only. Never opens a browser/GitHub URL.
     * @return true if an activity was started
     */
    fun openKernelSu(context: Context): Boolean {
        val pm = context.packageManager
        for (pkg in KSU_PACKAGES) {
            if (!isPackageInstalled(pm, pkg)) continue
            val launch = pm.getLaunchIntentForPackage(pkg) ?: continue
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
            return true
        }
        return false
    }

    fun openProjectReleases(context: Context) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(RELEASES_PAGE))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun isPackageInstalled(pm: PackageManager, pkg: String): Boolean =
        runCatching {
            pm.getPackageInfo(pkg, 0)
            true
        }.getOrDefault(false)

    private fun suBinaryPresent(): Boolean {
        val paths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/vendor/bin/su")
        return paths.any { File(it).exists() } ||
            runCatching {
                val p = Runtime.getRuntime().exec(arrayOf("which", "su"))
                p.waitFor(3, TimeUnit.SECONDS)
                p.inputStream.bufferedReader().readText().isNotBlank()
            }.getOrDefault(false)
    }

    private fun httpGet(url: String): String {
        val conn =
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "OnyxGestures/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/json")
            }
        try {
            if (conn.responseCode !in 200..299) {
                error("HTTP ${conn.responseCode} for $url")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private data class SuResult(val exitCode: Int, val output: String)

    private fun suCapture(cmd: String): String = suCaptureDetailed(cmd).output

    private fun suCaptureDetailed(cmd: String): SuResult {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val stdout = p.inputStream.bufferedReader().readText()
        val stderr = p.errorStream.bufferedReader().readText()
        val finished = p.waitFor(90, TimeUnit.SECONDS)
        if (!finished) {
            p.destroyForcibly()
            error("su timed out")
        }
        return SuResult(p.exitValue(), (stdout + stderr).trim())
    }
}
