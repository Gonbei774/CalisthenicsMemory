package io.github.gonbei774.calisthenicsmemory.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.github.gonbei774.calisthenicsmemory.R

class SoundPlayer(context: Context) {
    private val soundPool: SoundPool
    private val beepShortId: Int
    private val startCueId: Int
    private val setCompleteId: Int

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
        beepShortId = soundPool.load(context, R.raw.beep_short, 1)
        startCueId = soundPool.load(context, R.raw.start_cue, 1)
        setCompleteId = soundPool.load(context, R.raw.set_complete, 1)
    }

    fun playBeep() {
        soundPool.play(beepShortId, 1f, 1f, 1, 0, 1f)
    }

    fun playStartCue() {
        soundPool.play(startCueId, 1f, 1f, 1, 0, 1f)
    }

    fun playSetComplete() {
        soundPool.play(setCompleteId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
