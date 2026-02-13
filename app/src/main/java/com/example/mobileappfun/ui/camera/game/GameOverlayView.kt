package com.example.mobileappfun.ui.camera.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * Canvas HUD overlay: countdown "3-2-1-GO!", timer arc, score, round counter,
 * streak indicator, prompt emoji + instruction text, and feedback animations.
 */
class GameOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- State ---
    private var countdownValue: String? = null
    private var countdownScale = 1f
    private var promptEmoji: String? = null
    private var promptText: String? = null
    private var timerFraction = 1f // 1.0 = full, 0.0 = empty
    private var currentScore = 0
    private var displayedScore = 0
    private var currentRound = 0
    private var totalRounds = 0
    private var currentStreak = 0
    private var feedbackText: String? = null
    private var feedbackAlpha = 0f
    private var feedbackY = 0f
    private var pointsPopText: String? = null
    private var pointsPopAlpha = 0f
    private var pointsPopY = 0f
    private var isActive = false

    // --- Paints ---
    private val countdownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 160f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(8f, 0f, 4f, Color.BLACK)
    }

    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 100f
        textAlign = Paint.Align.CENTER
    }

    private val promptTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val timerArcBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }

    private val timerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val roundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)
        textSize = 32f
        textAlign = Paint.Align.RIGHT
        setShadowLayer(3f, 0f, 1f, Color.BLACK)
    }

    private val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 215, 0) // Gold
        textSize = 36f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(3f, 0f, 2f, Color.BLACK)
    }

    private val feedbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 52f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(6f, 0f, 3f, Color.BLACK)
    }

    private val pointsPopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(76, 175, 80)
        textSize = 44f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val overlayBgPaint = Paint().apply {
        color = Color.argb(60, 0, 0, 0)
    }

    private val timerRect = RectF()

    // --- Animators ---
    private var countdownAnimator: ValueAnimator? = null
    private var feedbackAnimator: ValueAnimator? = null
    private var pointsPopAnimator: ValueAnimator? = null
    private var scoreAnimator: ValueAnimator? = null

    // --- Public API ---

    fun showCountdown(value: Int) {
        isActive = true
        countdownValue = value.toString()
        promptEmoji = null
        promptText = null
        animateCountdown()
    }

    fun showGo() {
        countdownValue = "GO!"
        animateCountdown()
    }

    fun showPrompt(prompt: GesturePrompt, round: Int, total: Int) {
        countdownValue = null
        promptEmoji = prompt.emoji
        promptText = prompt.instruction
        currentRound = round
        totalRounds = total
        timerFraction = 1f
        invalidate()
    }

    fun updateTimer(millisRemaining: Long, totalMillis: Long) {
        timerFraction = millisRemaining.toFloat() / totalMillis.toFloat()
        invalidate()
    }

    fun updateScore(score: Int) {
        val oldScore = displayedScore
        currentScore = score
        scoreAnimator?.cancel()
        scoreAnimator = ValueAnimator.ofInt(oldScore, score).apply {
            duration = 400
            addUpdateListener {
                displayedScore = it.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    fun showMatchFeedback(points: Int, streak: Int) {
        currentStreak = streak
        showFeedback("Matched!", Color.rgb(76, 175, 80))
        showPointsPop("+$points")
    }

    fun showTimeoutFeedback() {
        currentStreak = 0
        showFeedback("Time's Up!", Color.rgb(244, 67, 54))
    }

    fun reset() {
        isActive = false
        countdownValue = null
        promptEmoji = null
        promptText = null
        feedbackText = null
        pointsPopText = null
        currentScore = 0
        displayedScore = 0
        currentRound = 0
        totalRounds = 0
        currentStreak = 0
        timerFraction = 1f
        countdownAnimator?.cancel()
        feedbackAnimator?.cancel()
        pointsPopAnimator?.cancel()
        scoreAnimator?.cancel()
        invalidate()
    }

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isActive) return

        // Countdown overlay
        if (countdownValue != null) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayBgPaint)
            drawCountdown(canvas)
            return
        }

        // Game HUD
        drawScoreBar(canvas)
        drawTimerArc(canvas)
        drawPrompt(canvas)
        drawStreak(canvas)
        drawFeedback(canvas)
        drawPointsPop(canvas)
    }

    private fun drawCountdown(canvas: Canvas) {
        val text = countdownValue ?: return
        val cx = width / 2f
        val cy = height / 2f

        canvas.save()
        canvas.scale(countdownScale, countdownScale, cx, cy)
        canvas.drawText(text, cx, cy + 50f, countdownPaint)
        canvas.restore()
    }

    private fun drawScoreBar(canvas: Canvas) {
        val padding = 24f
        val topY = 48f

        // Score (top-left)
        scorePaint.textSize = 48f
        canvas.drawText("$displayedScore", padding, topY + 48f, scorePaint)

        // Round counter (top-right)
        canvas.drawText("$currentRound/$totalRounds", width - padding, topY + 32f, roundPaint)
    }

    private fun drawTimerArc(canvas: Canvas) {
        val cx = width / 2f
        val arcSize = 70f
        val topY = 20f
        timerRect.set(cx - arcSize, topY, cx + arcSize, topY + arcSize * 2)

        // Background arc
        canvas.drawArc(timerRect, -90f, 360f, false, timerArcBgPaint)

        // Foreground arc with color based on time remaining
        timerArcPaint.color = when {
            timerFraction > 0.5f -> Color.rgb(76, 175, 80)   // Green
            timerFraction > 0.25f -> Color.rgb(255, 193, 7)  // Amber
            else -> Color.rgb(244, 67, 54)                    // Red
        }
        canvas.drawArc(timerRect, -90f, 360f * timerFraction, false, timerArcPaint)

        // Time text inside arc
        val timeText = String.format("%.1f", timerFraction * 5f)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = timerArcPaint.color
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(timeText, cx, topY + arcSize + 12f, timePaint)
    }

    private fun drawPrompt(canvas: Canvas) {
        val cx = width / 2f
        val promptY = height * 0.75f

        // Emoji
        promptEmoji?.let {
            canvas.drawText(it, cx, promptY, emojiPaint)
        }

        // Instruction text below emoji
        promptText?.let {
            canvas.drawText(it, cx, promptY + 50f, promptTextPaint)
        }
    }

    private fun drawStreak(canvas: Canvas) {
        if (currentStreak >= 2) {
            val padding = 24f
            val y = 110f
            val streakText = "\uD83D\uDD25 x$currentStreak"
            canvas.drawText(streakText, padding, y, streakPaint)
        }
    }

    private fun drawFeedback(canvas: Canvas) {
        val text = feedbackText ?: return
        if (feedbackAlpha <= 0f) return

        feedbackPaint.alpha = (feedbackAlpha * 255).toInt()
        canvas.drawText(text, width / 2f, feedbackY, feedbackPaint)
    }

    private fun drawPointsPop(canvas: Canvas) {
        val text = pointsPopText ?: return
        if (pointsPopAlpha <= 0f) return

        pointsPopPaint.alpha = (pointsPopAlpha * 255).toInt()
        canvas.drawText(text, width / 2f, pointsPopY, pointsPopPaint)
    }

    // --- Animations ---

    private fun animateCountdown() {
        countdownAnimator?.cancel()
        countdownScale = 2f
        countdownAnimator = ValueAnimator.ofFloat(2f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(2f)
            addUpdateListener {
                countdownScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun showFeedback(text: String, color: Int) {
        feedbackText = text
        feedbackPaint.color = color
        feedbackAnimator?.cancel()

        val startY = height * 0.45f
        val endY = height * 0.38f

        feedbackAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val progress = it.animatedValue as Float
                feedbackAlpha = if (progress < 0.7f) 1f else 1f - ((progress - 0.7f) / 0.3f)
                feedbackY = startY + (endY - startY) * progress
                invalidate()
            }
            start()
        }
    }

    private fun showPointsPop(text: String) {
        pointsPopText = text
        pointsPopAnimator?.cancel()

        val startY = height * 0.52f
        val endY = height * 0.42f

        pointsPopAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val progress = it.animatedValue as Float
                pointsPopAlpha = if (progress < 0.6f) 1f else 1f - ((progress - 0.6f) / 0.4f)
                pointsPopY = startY + (endY - startY) * progress
                invalidate()
            }
            start()
        }
    }
}
