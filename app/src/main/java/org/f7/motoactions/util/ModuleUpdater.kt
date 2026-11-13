package org.f7.motoactions.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.f7.motoactions.BuildConfig
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val zipUrl: String,
    val changelogUrl: String,
    val source: String,
)

sealed class UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult()

    data class UpToDate(val currentCode: Int, val remoteCode: Int) : UpdateCheckResult()

    data class Error(val message: String) : UpdateCheckResult()
}

object ModuleUpdater {
    const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/GustavoCaruzoGoncalves/moto-actions-poco-f7/main/module/update.json"
    const val GITHUB_LATEST_API =
        "https://api.github.com/repos/GustavoCaruzoGoncalves/moto-actions-poco-f7/releases/latest"
    const val RELEASES_PAGE =
        "https://github.com/GustavoCaruzoGoncalves/moto-actions-poco-f7/releases/latest"

    private val KSU_PACKAGES =
        listOf(
            "me.weishu.kernelsu",
            "com.rifsxd.ksunext",
            "com.android.system.ksu",
        )

    fun localVersionCode(context: Context): Int {
        val fromModule =
            runCatching {
                val out =
                    suCapture(
                        "grep '^versionCode=' /data/adb/modules/f7_moto_actions/module.prop 2>/dev/null | head -1",
                    )
                out.lineSequence()
                    .mapNotNull { line ->
                        val v = line.substringAfter("=", "").trim()
                        v.toIntOrNull()
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
        // Prefer hosted update.json (KernelSU-compatible), fall back to GitHub API.
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
        require(zip.isNotBlank()) { "update.json missing zipUrl" }
        return UpdateInfo(version, code, zip, changelog, "update.json")
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
            if (name.endsWith(".zip") && name.contains("f7_moto_actions", ignoreCase = true)) {
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

    fun downloadZip(context: Context, url: String, onProgress: ((Long, Long) -> Unit)? = null): File {
        val out = File(context.cacheDir, "f7_moto_actions_update.zip")
        if (out.exists()) out.delete()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "F7MotoActions/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode !in 200..299) {
                error("Download HTTP ${conn.responseCode}")
            }
            val total = conn.contentLengthLong
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(out).use { output ->
                    val buf = ByteArray(64 * 1024)
                    var readTotal = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        readTotal += n
                        onProgress?.invoke(readTotal, total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        require(out.length() > 1024) { "Downloaded ZIP too small" }
        return out
    }

    /** Install module ZIP via KernelSU `ksud`, which also runs customize.sh (pm install). */
    fun installWithRoot(zip: File): String {
        require(zip.exists()) { "ZIP missing" }
        val ksud =
            listOf(
                "/data/adb/ksu/bin/ksud",
                "/data/adb/ksud",
            ).firstOrNull { path ->
                suCapture("[ -x '$path' ] && echo ok").contains("ok")
            } ?: error("ksud not found — open KernelSU Manager to update the module")

        val staged = "/data/local/tmp/f7_moto_actions_update.zip"
        // Copy into world-readable tmp for root tools.
        suCapture("cp '${zip.absolutePath}' '$staged' && chmod 644 '$staged'")
        val log = suCapture("'$ksud' module install '$staged' 2>&1")
        suCapture("rm -f '$staged'")
        if (log.contains("Module installed successfully", ignoreCase = true) ||
            log.contains("Done", ignoreCase = true) ||
            log.contains("Installed", ignoreCase = true)
        ) {
            return log
        }
        // Some ksud builds only print to stdout without that phrase — accept non-empty without fatal error.
        if (log.contains("error", ignoreCase = true) &&
            !log.contains("installed successfully", ignoreCase = true)
        ) {
            error(log.take(800))
        }
        return log
    }

    fun rebootDevice() {
        suCapture("reboot")
    }

    fun openKernelSu(context: Context): Boolean {
        val pm = context.packageManager
        for (pkg in KSU_PACKAGES) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return true
            }
        }
        // Fallback: open releases page so user can install ZIP manually in KernelSU.
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return false
    }

    fun hasRoot(): Boolean =
        runCatching {
            suCapture("id").contains("uid=0")
        }.getOrDefault(false)

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "F7MotoActions/${BuildConfig.VERSION_NAME}")
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

    private fun suCapture(cmd: String): String {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val stdout = p.inputStream.bufferedReader().readText()
        val stderr = p.errorStream.bufferedReader().readText()
        val finished = p.waitFor(90, TimeUnit.SECONDS)
        if (!finished) {
            p.destroyForcibly()
            error("su timed out")
        }
        return (stdout + stderr).trim()
    }
}
