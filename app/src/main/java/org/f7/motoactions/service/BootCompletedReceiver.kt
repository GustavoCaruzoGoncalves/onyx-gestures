package org.f7.motoactions.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.f7.motoactions.MotoActionsApp
import org.f7.motoactions.util.EventLog

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = MotoActionsApp.instance.prefs
        if (!prefs.serviceEnabled) return
        EventLog.add("boot → start service (${intent?.action})")
        GestureService.start(context.applicationContext)
    }
}
