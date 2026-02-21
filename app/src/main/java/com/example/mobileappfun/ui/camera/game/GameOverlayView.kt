package com.example.mobileappfun.ui.camera.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.DrawableRes

/**
 * Canvas HUD overlay: countdown "3-2-1-GO!", timer arc, score, round counter,
 * streak indicator, monkey image prompt + instruction text, shake hint,
 * shake feedback, and match/timeout feedback animations.
 */
class GameOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- State ---
    private var countdownValue: String? = null
    private var countdownScale = 1f
    private var promptText: String? = null
    private var timerFraction = 1f
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

    // --- Monkey image state ---
    private var currentMonkeyBitmap: Bitmap? = null
    private var monkeyScale = 1f
    private val monkeyBitmapCache = HashMap<Int, Bitmap>()
    private val monkeyClipPath = Path()

    // --- Shake feedback state ---
    private var shakeFeedbackText: String? = null
    private var shakeFeedbackAlpha = 0f
    private var shakeFeedbackY = 0f

    // --- Paints ---
    private val countdownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 160f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(8f, 0f, 4f, Color.BLACK)
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
        color = Color.rgb(255, 215, 0)
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

    private val monkeyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.WHITE
    }

    private val monkeyGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val shakeHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 255, 255, 255)
        textSize = 22f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 1f, Color.BLACK)
    }

    private val shakeFeedbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(156, 39, 176) // Purple
        textSize = 52f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(6f, 0f, 3f, Color.BLACK)
    }

    private val timerRect = RectF()

    // --- Animators ---
    private var countdownAnimator: ValueAnimator? = null
    private var feedbackAnimator: ValueAnimator? = null
    private var pointsPopAnimator: ValueAnimator? = null
    private var scoreAnimator: ValueAnimator? = null
    private var monkeyAnimator: ValueAnimator? = null
    private var monkeyBorderAnimator: ValueAnimator? = null
    private var shakeFeedbackAnimator: ValueAnimator? = null

    // --- Public API ---

    fun showCountdown(value: Int) {
        isActive = true
        countdownValue = value.toString()
        promptText = null
        currentMonkeyBitmap = null
        animateCountdown()
    }

    fun showGo() {
        countdownValue = "GO!"
        animateCountdown()
    }

    fun showPrompt(prompt: GesturePrompt, round: Int, total: Int) {
        countdownValue = null
        promptText = prompt.instruction
        currentRound = round
        totalRounds = total
        timerFraction = 1f
        currentMonkeyBitmap = loadMonkeyBitmap(prompt.monkeyImageRes)
        monkeyBorderPaint.color = Color.WHITE
        animateMonkeyPopIn()
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
        animateMonkeyBorderPulse(Color.rgb(255, 215, 0))
    }

    fun showTimeoutFeedback() {
        currentStreak = 0
        showFeedback("Time's Up!", Color.rgb(244, 67, 54))
        animateMonkeyBorderPulse(Color.rgb(244, 67, 54))
    }

    /** Shows a "Skipped!" animation when the player shakes to skip. */
    fun showShakeFeedback() {
        showShakePop("Skipped! \uD83D\uDE49")  // 🙈
        animateMonkeyBorderPulse(Color.rgb(156, 39, 176))
    }

    fun reset() {
        isActive = false
        countdownValue = null
        promptText = null
        feedbackText = null
        pointsPopText = null
        shakeFeedbackText = null
        currentMonkeyBitmap = null
        currentScore = 0
        displayedScore = 0
        currentRound = 0
        totalRounds = 0
        currentStreak = 0
        timerFraction = 1f
        monkeyScale = 1f
        monkeyBorderPaint.color = Color.WHITE
        countdownAnimator?.cancel()
        feedbackAnimator?.cancel()
        pointsPopAnimator?.cancel()
        scoreAnimator?.cancel()
        monkeyAnimator?.cancel()
        monkeyBorderAnimator?.cancel()
        shakeFeedbackAnimator?.cancel()
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
        drawMonkeyPrompt(canvas)
        drawStreak(canvas)
        drawFeedback(canvas)
        drawPointsPop(canvas)
        drawShakeFeedback(canvas)
        drawShakeHint(canvas)
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

        scorePaint.textSize = 48f
        canvas.drawText("$displayedScore", padding, topY + 48f, scorePaint)
        canvas.drawText("$currentRound/$totalRounds", width - padding, topY + 32f, roundPaint)
    }

    private fun drawTimerArc(canvas: Canvas) {
        val cx = width / 2f
        val arcSize = 70f
        val topY = 20f
        timerRect.set(cx - arcSize, topY, cx + arcSize, topY + arcSize * 2)

        canvas.drawArc(timerRect, -90f, 360f, false, timerArcBgPaint)

        timerArcPaint.color = when {
            timerFraction > 0.5f -> Color.rgb(76, 175, 80)
            timerFraction > 0.25f -> Color.rgb(255, 193, 7)
            else -> Color.rgb(244, 67, 54)
        }
        canvas.drawArc(timerRect, -90f, 360f * timerFraction, false, timerArcPaint)

        val timeText = String.format("%.1f", timerFraction * 5f)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = timerArcPaint.color
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(timeText, cx, topY + arcSize + 12f, timePaint)
    }

    private fun drawMonkeyPrompt(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val cx = width / 2f
        val monkeyRadius = minOf(width * 0.20f, height * 0.13f)
        val baseY = height * 0.74f
        val scaledRadius = monkeyRadius * monkeyScale

        val bmp = currentMonkeyBitmap
        if (bmp != null && scaledRadius > 0) {
            val dst = RectF(
                cx - scaledRadius, baseY - scaledRadius,
                cx + scaledRadius, baseY + scaledRadius
            )

            // Gold glow rings behind the monkey
            for (i in 3 downTo 1) {
                monkeyGlowPaint.color = Color.argb(20 * i, 255, 215, 0)
                monkeyGlowPaint.strokeWidth = 5f * i
                canvas.drawCircle(cx, baseY, scaledRadius + 5f * i, monkeyGlowPaint)
            }

            // Clip monkey bitmap to circle
            monkeyClipPath.reset()
            monkeyClipPath.addCircle(cx, baseY, scaledRadius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(monkeyClipPath)
            canvas.drawBitmap(bmp, null, dst, null)
            canvas.restore()

            // White/colored border circle
            canvas.drawCircle(cx, baseY, scaledRadius, monkeyBorderPaint)
        }

        // Instruction text below monkey
        promptText?.let {
            val textY = baseY + monkeyRadius + 52f
            canvas.drawText(it, cx, textY, promptTextPaint)
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

    private fun drawShakeFeedback(canvas: Canvas) {
        val text = shakeFeedbackText ?: return
        if (shakeFeedbackAlpha <= 0f) return

        shakeFeedbackPaint.alpha = (shakeFeedbackAlpha * 255).toInt()
        canvas.drawText(text, width / 2f, shakeFeedbackY, shakeFeedbackPaint)
    }

    private fun drawShakeHint(canvas: Canvas) {
        if (height == 0) return
        canvas.drawText("\uD83D\uDCF1 Shake to skip", width / 2f, height * 0.93f, shakeHintPaint)
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

    private fun animateMonkeyPopIn() {
        monkeyAnimator?.cancel()
        monkeyScale = 0f
        monkeyAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            interpolator = OvershootInterpolator(1.5f)
            addUpdateListener {
                monkeyScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** Pulses the monkey border to `color` then fades back to white. */
    private fun animateMonkeyBorderPulse(color: Int) {
        monkeyBorderAnimator?.cancel()
        monkeyBorderPaint.color = color
        monkeyBorderAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 600
            addUpdateListener {
                val f = it.animatedValue as Float
                val r = (Color.red(color) * f + 255 * (1f - f)).toInt()
                val g = (Color.green(color) * f + 255 * (1f - f)).toInt()
                val b = (Color.blue(color) * f + 255 * (1f - f)).toInt()
                monkeyBorderPaint.color = Color.rgb(r, g, b)
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

    private fun showShakePop(text: String) {
        shakeFeedbackText = text
        shakeFeedbackAnimator?.cancel()

        val startY = height * 0.55f
        val endY = height * 0.45f

        shakeFeedbackAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val progress = it.animatedValue as Float
                shakeFeedbackAlpha = if (progress < 0.6f) 1f else 1f - ((progress - 0.6f) / 0.4f)
                shakeFeedbackY = startY + (endY - startY) * progress
                invalidate()
            }
            start()
        }
    }

    private fun loadMonkeyBitmap(@DrawableRes resId: Int): Bitmap? {
        monkeyBitmapCache[resId]?.let { return it }
        return try {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bmp = BitmapFactory.decodeResource(context.resources, resId, options)
            if (bmp != null) monkeyBitmapCache[resId] = bmp
            bmp
        } catch (e: Exception) {
            null
        }
    }
}
