package com.bahalabs.sortopia.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.bahalabs.sortopia.R

/**
 * A short musical welcome: when the home screen first appears after the app opens, the theme
 * plays for a couple of seconds and fades out. It never loops and doesn't come back until the
 * next launch; gameplay keeps only its sound effects.
 */
class Music(context: Context) {
    private val player: MediaPlayer? = MediaPlayer.create(
        context,
        R.raw.music,
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build(),
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).generateAudioSessionId(),
    )

    private val handler = Handler(Looper.getMainLooper())
    private var played = false

    /** Plays the welcome once per launch, if the player has music on. */
    fun playWelcomeOnce(musicOn: Boolean) {
        if (played || !musicOn) return
        played = true
        val p = player ?: return
        p.setVolume(VOLUME, VOLUME)
        p.seekTo(0)
        p.start()
        // Hold, then fade out in small steps and stop.
        for (step in 1..FADE_STEPS) {
            handler.postDelayed({
                val v = VOLUME * (1f - step / FADE_STEPS.toFloat())
                if (p.isPlaying) p.setVolume(v, v)
                if (step == FADE_STEPS) stop()
            }, HOLD_MS + step * (FADE_MS / FADE_STEPS))
        }
    }

    /** Silences the welcome at once (app sent to the background, music turned off). */
    fun stop() {
        handler.removeCallbacksAndMessages(null)
        val p = player ?: return
        if (p.isPlaying) p.pause()
        p.seekTo(0)
    }

    private companion object {
        const val VOLUME = 0.4f
        const val HOLD_MS = 1_800L
        const val FADE_MS = 900L
        const val FADE_STEPS = 9
    }
}
