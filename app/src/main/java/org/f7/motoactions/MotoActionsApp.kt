package org.f7.motoactions

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import org.f7.motoactions.service.GestureService
import org.f7.motoactions.service.KeepAliveReceiver
import org.f7.motoactions.util.EnergyMonitor
import org.f7.motoactions.util.Prefs

class MotoActionsApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var energy: EnergyMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        energy = EnergyMonitor(this)
        energy.start()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps gesture recognition alive under Android background limits"
                setShowBadge(false)
            },
        )
        // Any process resurrection should re-arm gestures (HyperOS freezes/kills FGS).
        if (prefs.serviceEnabled) {
            runCatching { GestureService.start(this) }
            KeepAliveReceiver.schedule(this)
        }
    }

    companion object {
        const val CHANNEL_ID = "gesture_engine"
        lateinit var instance: MotoActionsApp
            private set
    }
}
