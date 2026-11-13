package org.f7.motoactions.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import org.f7.motoactions.MotoActionsApp
import org.f7.motoactions.util.EventLog

/**
 * External pulse so HyperOS/MIUI cannot leave us dead after an FGS kill.
 * Moto Actions on stock/Lineage never needs this — it is a persistent system app.
 */
class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val prefs = MotoActionsApp.instance.prefs
        if (!prefs.serviceEnabled) return
        EventLog.add("keepalive pulse (${intent?.action})")
        GestureService.start(context.applicationContext)
        schedule(context.applicationContext)
    }

    companion object {
        const val ACTION = "org.f7.motoactions.KEEPALIVE"
        private const val INTERVAL_MS = 45_000L
        private const val REQ = 91

        fun schedule(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val pi =
                PendingIntent.getBroadcast(
                    context,
                    REQ,
                    Intent(context, KeepAliveReceiver::class.java).setAction(ACTION),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val trigger = SystemClock.elapsedRealtime() + INTERVAL_MS
            runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
            }.recoverCatching {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
            }.onFailure {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
            }
        }

        fun cancel(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val pi =
                PendingIntent.getBroadcast(
                    context,
                    REQ,
                    Intent(context, KeepAliveReceiver::class.java).setAction(ACTION),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            am.cancel(pi)
        }
    }
}
