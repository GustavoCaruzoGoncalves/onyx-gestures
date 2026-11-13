package org.f7.motoactions.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log

class TorchController(context: Context) {
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    @Volatile var torchEnabled: Boolean = false
        private set
    @Volatile private var rearCameraId: String? = null

    private val callback =
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == rearCameraId) torchEnabled = enabled
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                if (cameraId == rearCameraId) torchEnabled = false
            }
        }

    init {
        runCatching {
            for (id in cameraManager.cameraIdList) {
                val facing =
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.LENS_FACING)
                val flash =
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (facing == CameraCharacteristics.LENS_FACING_BACK && flash) {
                    rearCameraId = id
                    break
                }
            }
            cameraManager.registerTorchCallback(callback, null)
        }.onFailure { Log.e(TAG, "torch init failed", it) }
    }

    fun toggle(): Boolean {
        val id = rearCameraId ?: return false
        return runCatching {
            val next = !torchEnabled
            cameraManager.setTorchMode(id, next)
            torchEnabled = next
            true
        }.getOrElse {
            Log.e(TAG, "setTorchMode failed", it)
            EventLog.add("torch error: ${it.message}")
            false
        }
    }

    fun release() {
        runCatching { cameraManager.unregisterTorchCallback(callback) }
    }

    companion object {
        private const val TAG = "TorchController"
    }
}
