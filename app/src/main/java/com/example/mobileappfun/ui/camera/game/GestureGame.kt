package com.example.mobileappfun.ui.camera.game

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper

/**
 * Core state machine for the gesture matching game.
 * Manages rounds, scoring, timer, and gesture hold detection.
 */
class GestureGame(private val callback: GameCallback) {

    enum class State {
        IDLE, COUNTDOWN, PLAYING_ROUND, MATCHED, TIMEOUT, GAME_OVER
    }

    interface GameCallback {
        fun onCountdown(value: Int) // 3, 2, 1
        fun onCountdownGo()
        fun onRoundStart(round: Int, totalRounds: Int, prompt: GesturePrompt)
        fun onTimerTick(millisRemaining: Long, totalMillis: Long)
        fun onGestureMatched(points: Int, totalScore: Int, streak: Int)
        fun onGestureTimeout()
        fun onGameOver(score: Int, correctCount: Int, totalRounds: Int, bestStreak: Int)
        fun onStateChanged(state: State)
    }

    companion object {
        const val TOTAL_ROUNDS = 10
        const val ROUND_TIME_MS = 5000L
        const val HOLD_DURATION_MS = 600L
        const val BASE_POINTS = 10
        const val MAX_TIME_BONUS = 10
        const val STREAK_BONUS = 5
        const val STREAK_THRESHOLD = 3
        private const val MATCH_PAUSE_MS = 1200L
        private const val TIMEOUT_PAUSE_MS = 1000L
        private const val COUNTDOWN_INTERVAL_MS = 1000L
    }

    var state = State.IDLE
        private set

    private var currentRound = 0
    private var score = 0
    private var correctCount = 0
    private var currentStreak = 0
    private var bestStreak = 0
    private var currentPrompt: GesturePrompt? = null
    private var lastPrompt: GesturePrompt? = null

    private var roundTimer: CountDownTimer? = null
    private var holdStartTime = 0L
    private var isHolding = false

    private val handler = Handler(Looper.getMainLooper())

    fun startGame() {
        resetState()
        changeState(State.COUNTDOWN)
        runCountdown()
    }

    fun stopGame() {
        roundTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        resetState()
        changeState(State.IDLE)
    }

    /** Skip the current round (e.g. triggered by shake). Counts as a timeout. */
    fun skipRound() {
        if (state != State.PLAYING_ROUND) return
        roundTimer?.cancel()
        onTimeout()
    }

    fun onGestureDetected(categoryName: String?) {
        if (state != State.PLAYING_ROUND) return
        val prompt = currentPrompt ?: return

        if (categoryName == prompt.categoryName) {
            if (!isHolding) {
                isHolding = true
                holdStartTime = System.currentTimeMillis()
            } else {
                val heldFor = System.currentTimeMillis() - holdStartTime
                if (heldFor >= HOLD_DURATION_MS) {
                    onMatch()
                }
            }
        } else {
            // Wrong gesture or no gesture — reset hold
            isHolding = false
            holdStartTime = 0L
        }
    }

    private fun runCountdown() {
        var countValue = 3
        callback.onCountdown(countValue)

        val countdownTimer = object : CountDownTimer(3 * COUNTDOWN_INTERVAL_MS, COUNTDOWN_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                countValue--
                if (countValue > 0) {
                    callback.onCountdown(countValue)
                }
            }

            override fun onFinish() {
                callback.onCountdownGo()
                handler.postDelayed({ startNextRound() }, 500)
            }
        }
        countdownTimer.start()
    }

    private fun startNextRound() {
        currentRound++
        if (currentRound > TOTAL_ROUNDS) {
            endGame()
            return
        }

        val prompt = GesturePrompt.randomExcluding(lastPrompt)
        currentPrompt = prompt
        lastPrompt = prompt
        isHolding = false
        holdStartTime = 0L

        changeState(State.PLAYING_ROUND)
        callback.onRoundStart(currentRound, TOTAL_ROUNDS, prompt)

        roundTimer?.cancel()
        roundTimer = object : CountDownTimer(ROUND_TIME_MS, 50) {
            override fun onTick(millisUntilFinished: Long) {
                callback.onTimerTick(millisUntilFinished, ROUND_TIME_MS)
            }

            override fun onFinish() {
                onTimeout()
            }
        }.start()
    }

    private fun onMatch() {
        roundTimer?.cancel()
        changeState(State.MATCHED)

        currentStreak++
        if (currentStreak > bestStreak) bestStreak = currentStreak
        correctCount++

        // Calculate points
        val timeBonus = calculateTimeBonus()
        var points = BASE_POINTS + timeBonus
        if (currentStreak >= STREAK_THRESHOLD) {
            points += STREAK_BONUS
        }
        score += points

        callback.onGestureMatched(points, score, currentStreak)

        handler.postDelayed({ startNextRound() }, MATCH_PAUSE_MS)
    }

    private fun onTimeout() {
        changeState(State.TIMEOUT)
        currentStreak = 0
        callback.onGestureTimeout()

        handler.postDelayed({ startNextRound() }, TIMEOUT_PAUSE_MS)
    }

    private fun endGame() {
        changeState(State.GAME_OVER)
        callback.onGameOver(score, correctCount, TOTAL_ROUNDS, bestStreak)
    }

    private fun calculateTimeBonus(): Int {
        // Time bonus based on how quickly the gesture was matched
        // roundTimer still has the remaining time info, but we track by hold start
        val elapsed = System.currentTimeMillis() - (holdStartTime - HOLD_DURATION_MS)
        val fraction = 1.0 - (elapsed.toDouble() / ROUND_TIME_MS).coerceIn(0.0, 1.0)
        return (fraction * MAX_TIME_BONUS).toInt()
    }

    private fun changeState(newState: State) {
        state = newState
        callback.onStateChanged(newState)
    }

    private fun resetState() {
        roundTimer?.cancel()
        currentRound = 0
        score = 0
        correctCount = 0
        currentStreak = 0
        bestStreak = 0
        currentPrompt = null
        lastPrompt = null
        isHolding = false
        holdStartTime = 0L
    }
}
