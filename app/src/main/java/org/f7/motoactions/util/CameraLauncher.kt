package org.f7.motoactions.util

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import org.f7.motoactions.R
import org.f7.motoactions.ui.CameraTrampolineActivity
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Launch MGC / system camera from background.
 *
 * On PixelOS 17, BAL blocks [startActivity] from FGS when the UI is not visible;
 * full-screen intents become a heads-up only while the screen is on — hence the
 * "Opening camera…" toast-like notification without an activity.
 *
 * Strategy:
 * 1. Flag file for KernelSU [service.sh] root watcher (always works with module).
 * 2. PendingIntent.send + BAL allow targeting MGC explicitly.
 * 3. startActivity with ActivityOptions BAL (API 34+).
 * 4. su am start (if KSU grants this app).
 * 5. Heads-up / FSI as last resort (user can tap).
 */
class CameraLauncher(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun launch() {
        ensureCameraChannel()
        EventLog.add("camera launch requested")

        // 1) Ask module root helper (service.sh) — preferred on KernelSU.
        if (writeModuleCameraRequest()) {
            EventLog.add("camera request flag written for service.sh")
        }

        // 2) BAL-aware PendingIntent → MGC
        if (sendCameraPendingIntent()) {
            EventLog.add("camera via PendingIntent+BAL")
            return
        }

        // 3) Direct startActivity with options
        if (tryStartCameraActivities()) {
            EventLog.add("camera via startActivity")
            return
        }

        // 4) Root from app process (needs KSU allow for this UID)
        if (tryRootAmStart()) {
            EventLog.add("camera via su am start")
            return
        }

        // 5) Notification the user can tap (and FSI when locked)
        postLaunchNotification()
        EventLog.add("camera posted tap/FSI notification (await helper or tap)")
        // Give the module watcher a moment; retry BAL once.
        mainHandler.postDelayed({ sendCameraPendingIntent() }, 500)
    }

    private fun writeModuleCameraRequest(): Boolean {
        return runCatching {
            val f = File(context.filesDir, REQUEST_FILE)
            f.parentFile?.mkdirs()
            f.writeText(System.currentTimeMillis().toString())
            // World-readable so root service.sh can see it across CE storage.
            f.setReadable(true, false)
            true
        }.onFailure {
            Log.w(TAG, "request flag failed", it)
        }.getOrDefault(false)
    }

    private fun cameraIntents(): List<Intent> {
        val flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
        return listOf(
            Intent().setComponent(ComponentName(MGC_PKG, MGC_SECURE)).addFlags(flags),
            Intent().setComponent(ComponentName(MGC_PKG, MGC_IMAGE)).addFlags(flags),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE).addFlags(flags),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).setPackage(MGC_PKG).addFlags(flags),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(flags),
            Intent(context, CameraTrampolineActivity::class.java).addFlags(flags),
        )
    }

    private fun sendCameraPendingIntent(): Boolean {
        for ((idx, intent) in cameraIntents().withIndex()) {
            val ok =
                runCatching {
                    val pi =
                        PendingIntent.getActivity(
                            context,
                            REQ_CAMERA + idx,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    if (Build.VERSION.SDK_INT >= 34) {
                        val opts =
                            ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                            )
                        pi.send(context, 0, null, null, null, null, opts.toBundle())
                    } else {
                        pi.send()
                    }
                    true
                }.onFailure {
                    Log.w(TAG, "PI send failed for $intent", it)
                }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }

    private fun tryStartCameraActivities(): Boolean {
        for (intent in cameraIntents()) {
            val ok =
                runCatching {
                    if (Build.VERSION.SDK_INT >= 34) {
                        val opts =
                            ActivityOptions.makeBasic().setPendingIntentCreatorBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                            )
                        context.startActivity(intent, opts.toBundle())
                    } else {
                        context.startActivity(intent)
                    }
                    true
                }.onFailure {
                    Log.w(TAG, "startActivity failed $intent", it)
                }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }

    private fun postLaunchNotification() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val intent =
            Intent().setComponent(ComponentName(MGC_PKG, MGC_SECURE)).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            )
        val pi =
            PendingIntent.getActivity(
                context,
                REQ_CAMERA,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val n =
            NotificationCompat.Builder(context, CHANNEL_CAMERA)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.camera_launch_notification))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setTimeoutAfter(6_000)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .build()
        nm.notify(NOTIF_CAMERA, n)
    }

    private fun ensureCameraChannel() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CAMERA,
                context.getString(R.string.camera_launch_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Opens camera on Twist"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    private fun tryRootAmStart(): Boolean {
        val commands =
            listOf(
                "am start -n $MGC_PKG/$MGC_SECURE --activity-clear-top",
                "am start -n $MGC_PKG/$MGC_IMAGE --activity-clear-top",
                "am start -a android.media.action.STILL_IMAGE_CAMERA_SECURE --activity-clear-top",
            )
        val suInvokers =
            listOf(
                listOf("/system/bin/su", "-c"),
                listOf("su", "-c"),
                listOf("/system/bin/su", "0", "sh", "-c"),
            )
        for (inv in suInvokers) {
            val bin = inv.first()
            if (bin.startsWith("/") && !File(bin).exists()) continue
            for (cmd in commands) {
                val ok =
                    runCatching {
                        val args = ArrayList(inv).also { it.add(cmd) }
                        val p = ProcessBuilder(args).redirectErrorStream(true).start()
                        val finished = p.waitFor(3, TimeUnit.SECONDS)
                        val code = if (finished) p.exitValue() else -1
                        if (!finished) p.destroyForcibly()
                        Log.i(TAG, "root → $code ($cmd)")
                        code == 0
                    }.getOrDefault(false)
                if (ok) return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "CameraLauncher"
        private const val CHANNEL_CAMERA = "camera_launch"
        private const val NOTIF_CAMERA = 48
        private const val REQ_CAMERA = 4800
        const val REQUEST_FILE = "request_camera"
        private const val MGC_PKG = "com.android.MGC"
        private const val MGC_SECURE = "com.android.camera.SecureCameraActivity"
        private const val MGC_IMAGE = "com.android.camera.CameraImageActivity"
    }
}
