package org.f7.motoactions.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.WindowManager
import org.f7.motoactions.util.EventLog

/**
 * Transparent bridge used when BAL allows activity starts, or via full-screen intent.
 * Turns the screen on, then hands off to the system camera.
 */
class CameraTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val kg = getSystemService(KeyguardManager::class.java)
        if (kg != null && kg.isKeyguardLocked) {
            kg.requestDismissKeyguard(this, null)
        }

        val launched = launchCamera()
        if (!launched) {
            EventLog.add("trampoline: all camera intents failed")
        }
        // Delay finish so the camera activity can take focus (finish() too early drops BAL).
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, 600)
    }

    private fun launchCamera(): Boolean {
        val candidates =
            listOf(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE),
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            )
        for (base in candidates) {
            val i =
                Intent(base).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION,
                    )
                }
            if (runCatching { startActivity(i); true }.getOrDefault(false)) {
                EventLog.add("trampoline → ${i.action}")
                return true
            }
        }
        return false
    }
}
