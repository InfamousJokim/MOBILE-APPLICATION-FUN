package com.example.mobileappfun.ui.camera.game

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Manages all game sound effects using ToneGenerator.
 * For background music, add a file named "game_music.mp3" to res/raw/ and
 * uncomment the MediaPlayer block in startBackgroundMusic().
 */
class GameSoundManager(private val context: Context) {

    private var toneGen: ToneGenerator? = null

    fun init() {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: RuntimeException) {
            // ToneGenerator not available on this device
        }
    }

    /** Play a countdown beep. step = 3, 2, or 1. */
    fun playCountdownBeep(step: Int) {
        val tone = if (step > 1) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_CDMA_PIP
        play(tone, 250)
    }

    /** Play the "GO!" fanfare sound. */
    fun playGo() {
        play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
    }

    /** Play a success chime when a gesture is matched. */
    fun playMatch() {
        play(ToneGenerator.TONE_PROP_ACK, 200)
    }

    /** Play an error buzz when time runs out. */
    fun playTimeout() {
        play(ToneGenerator.TONE_PROP_NACK, 400)
    }

    /** Play a game-over fanfare. */
    fun playGameOver() {
        play(ToneGenerator.TONE_CDMA_ABBR_REORDER, 800)
    }

    /** Play a fun skip sound when the player shakes to skip. */
    fun playShakeSkip() {
        play(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 250)
    }

    fun release() {
        try {
            toneGen?.release()
        } catch (e: Exception) { /* ignore */ }
        toneGen = null
    }

    private fun play(tone: Int, durationMs: Int) {
        try {
            toneGen?.startTone(tone, durationMs)
        } catch (e: Exception) { /* ignore if audio unavailable */ }
    }
}
