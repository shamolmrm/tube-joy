package com.bahalabs.sortopia.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.bahalabs.sortopia.R

enum class Sfx(val res: Int) {
    SELECT(R.raw.select),
    POUR(R.raw.pour),
    CLINK(R.raw.clink),
    COMPLETE(R.raw.complete),
    WIN(R.raw.win),
    INVALID(R.raw.invalid),
}

/** Short effects through SoundPool (low latency, mixes with the player's own music). */
class Sounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = Sfx.entries.associateWith { pool.load(context, it.res, 1) }

    var enabled = true

    fun play(sfx: Sfx, volume: Float = 0.8f) {
        if (!enabled) return
        ids[sfx]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }
}
