package io.github.gonbei774.calisthenicsmemory.util

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.delay

class FlashController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = try {
        cameraManager.cameraIdList.firstOrNull()
    } catch (e: Exception) {
        null
    }

    val isFlashAvailable: Boolean = context.packageManager
        .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    // Short flash for countdown beeps (100ms)
    suspend fun flashShort() {
        if (cameraId == null) return
        try {
            cameraManager.setTorchMode(cameraId, true)
            delay(100L)
            cameraManager.setTorchMode(cameraId, false)
        } catch (e: Exception) {
            // Ignore errors (e.g., flash in use by another app)
        }
    }

    // Longer flash for completion (200ms)
    suspend fun flashComplete() {
        if (cameraId == null) return
        try {
            cameraManager.setTorchMode(cameraId, true)
            delay(200L)
            cameraManager.setTorchMode(cameraId, false)
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    // Long flash for set completion (covers entire triple beep sound)
    suspend fun flashSetComplete() {
        if (cameraId == null) return
        try {
            cameraManager.setTorchMode(cameraId, true)
            // ピピピx3の音より長め: 約3.3秒
            delay(3300L)
            cameraManager.setTorchMode(cameraId, false)
        } catch (e: Exception) {
            // Ignore errors
        } finally {
            // Ensure flash is turned off
            turnOff()
        }
    }

    fun turnOff() {
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, false)
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }
}