package org.f7.motoactions.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import org.f7.motoactions.MotoActionsApp
import org.f7.motoactions.R
import org.f7.motoactions.ui.MainActivity
import org.f7.motoactions.util.CameraLauncher
import org.f7.motoactions.util.EventLog
import org.f7.motoactions.util.Prefs
import org.f7.motoactions.util.TorchController

class GestureService : Service() {
    private lateinit var prefs: Prefs
    private lateinit var torch: TorchController
    private lateinit var camera: CameraLauncher
    private lateinit var pipeline: SensorPipeline

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                Prefs.KEY_SERVICE -> {
                    if (!prefs.serviceEnabled) {
                        KeepAliveReceiver.cancel(this)
                        stopSelf()
                    } else {
                        KeepAliveReceiver.schedule(this)
                    }
                }
                else -> pipeline.applyThresholds()
            }
        }

    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> pipeline.onScreenChanged(true)
                    Intent.ACTION_SCREEN_OFF -> pipeline.onScreenChanged(false)
                    Intent.ACTION_USER_PRESENT,
                    PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED,
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
                    -> {
                        EventLog.add("power nudge ${intent?.action}")
                        pipeline.watchdogTick()
                        if (prefs.serviceEnabled) KeepAliveReceiver.schedule(this@GestureService)
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        prefs = MotoActionsApp.instance.prefs
        torch = TorchController(this)
        camera = CameraLauncher(this)
        pipeline = SensorPipeline(this, prefs, torch, camera)
        prefs.register(prefListener)
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
        startAsForeground()
        if (prefs.serviceEnabled) {
            pipeline.start()
            KeepAliveReceiver.schedule(this)
            EventLog.add("GestureService started")
        } else {
            stopSelf()
        }
    }

    private fun startAsForeground() {
        val pi =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification: Notification =
            NotificationCompat.Builder(this, MotoActionsApp.CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Re-assert FGS notification on every start (HyperOS may demote).
        startAsForeground()
        when (intent?.action) {
            ACTION_STOP -> {
                prefs.serviceEnabled = false
                KeepAliveReceiver.cancel(this)
                stopSelf()
            }
            ACTION_START_CAPTURE ->
                pipeline.setCapturing(true, intent.getStringExtra(EXTRA_LABEL) ?: "idle")
            ACTION_STOP_CAPTURE -> pipeline.setCapturing(false)
            ACTION_WATCHDOG -> pipeline.watchdogTick()
            else -> {
                if (prefs.serviceEnabled) {
                    pipeline.applyThresholds()
                    KeepAliveReceiver.schedule(this)
                }
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (prefs.serviceEnabled) {
            EventLog.add("task removed → reschedule")
            KeepAliveReceiver.schedule(this)
            start(applicationContext)
        }
    }

    override fun onDestroy() {
        prefs.unregister(prefListener)
        runCatching { unregisterReceiver(screenReceiver) }
        pipeline.stop()
        torch.release()
        if (prefs.serviceEnabled) {
            // Ask the OS to resurrect us; alarm covers HyperOS FGS kills.
            KeepAliveReceiver.schedule(this)
        }
        EventLog.add("GestureService stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 47
        const val ACTION_STOP = "org.f7.motoactions.STOP"
        const val ACTION_START_CAPTURE = "org.f7.motoactions.START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "org.f7.motoactions.STOP_CAPTURE"
        const val ACTION_WATCHDOG = "org.f7.motoactions.WATCHDOG"
        const val EXTRA_LABEL = "label"

        fun start(context: Context) {
            val i = Intent(context, GestureService::class.java)
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, GestureService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
