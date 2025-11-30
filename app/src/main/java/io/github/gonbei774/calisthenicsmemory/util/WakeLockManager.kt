package io.github.gonbei774.calisthenicsmemory.util

import android.content.Context
import android.os.PowerManager

class WakeLockManager(context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CalisthenicsMemory::WorkoutTimer"
            )
        }
        wakeLock?.acquire(60 * 60 * 1000L) // Max 1 hour
    }

    fun release() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}