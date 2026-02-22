package com.example.mobileappfun.ui.camera.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.mobileappfun.R

/**
 * Manages all game audio:
 *  - Background music (MediaPlayer, looping) → res/raw/bgm_game.mp3
 *  - Correct gesture sound (SoundPool)        → res/raw/sound_correct.wav
 *  - Failed / timeout sound (SoundPool)       → res/raw/sound_failed.wav
 *  - Game completed sound (SoundPool)         → res/raw/sound_game_complete.wav
 */
class GameSoundManager(private val context: Context) {

    // Background music
    private var mediaPlayer: MediaPlayer? = null

    // Short sound effects
    private var soundPool: SoundPool? = null
    private var soundCorrect: Int = 0
    private var soundFailed: Int = 0
    private var soundGameComplete: Int = 0

    fun init() {
        initSoundPool()
        startBackgroundMusic()
    }

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        soundCorrect = soundPool?.load(context, R.raw.sound_correct, 1) ?: 0
        soundFailed = soundPool?.load(context, R.raw.sound_failed, 1) ?: 0
        soundGameComplete = soundPool?.load(context, R.raw.sound_game_complete, 1) ?: 0
    }

    private fun startBackgroundMusic() {
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.bgm_game)?.apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
                start()
            }
        } catch (e: Exception) {
            // Audio not available on this device
        }
    }

    /** Played when a gesture is correctly matched. */
    fun playMatch() {
        soundPool?.play(soundCorrect, 1f, 1f, 1, 0, 1f)
    }

    /** Played when a round times out or the player shakes to skip. */
    fun playFailed() {
        soundPool?.play(soundFailed, 1f, 1f, 1, 0, 1f)
    }

    /** Played at the end of the game. Stops background music first. */
    fun playGameOver() {
        stopBackgroundMusic()
        soundPool?.play(soundGameComplete, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        stopBackgroundMusic()
        try {
            soundPool?.release()
        } catch (e: Exception) { /* ignore */ }
        soundPool = null
    }

    private fun stopBackgroundMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) { /* ignore */ }
        mediaPlayer = null
    }
}
