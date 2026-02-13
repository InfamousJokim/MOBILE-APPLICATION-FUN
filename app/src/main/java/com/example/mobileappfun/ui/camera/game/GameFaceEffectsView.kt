package com.example.mobileappfun.ui.camera.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.google.mlkit.vision.face.Face

/**
 * Draws emoji overlays on the detected face:
 * - Ghost prompt emoji during play (semi-transparent, above head)
 * - Crown emoji on correct match
 * - Sad face on timeout
 * - Glow ring effect around face on match/timeout
 *
 * Coordinate mapping mirrors FaceOverlayView exactly.
 */
class GameFaceEffectsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var faces: List<Face> = emptyList()
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var isFrontCamera: Boolean = false

    // Effect state
    private var activeEmoji: String? = null
    private var emojiAlpha = 255
    private var emojiScale = 1f
    private var ghostEmoji: String? = null
    private var glowColor: Int = Color.TRANSPARENT
    private var glowAlpha = 0f
    private var isActive = false

    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 80f
        textAlign = Paint.Align.CENTER
    }

    private val ghostEmojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 60f
        textAlign = Paint.Align.CENTER
        alpha = 100
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private var emojiAnimator: ValueAnimator? = null
    private var glowAnimator: ValueAnimator? = null

    fun setFaceData(faces: List<Face>, imageWidth: Int, imageHeight: Int, isFrontCamera: Boolean) {
        this.faces = faces
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.isFrontCamera = isFrontCamera
        if (isActive) invalidate()
    }

    fun showMatchEffect() {
        isActive = true
        activeEmoji = "\uD83D\uDC51" // Crown
        glowColor = Color.rgb(76, 175, 80) // Green
        ghostEmoji = null
        animateEmoji()
        animateGlow()
    }

    fun showTimeoutEffect() {
        isActive = true
        activeEmoji = "\uD83D\uDE1E" // Sad face
        glowColor = Color.rgb(244, 67, 54) // Red
        ghostEmoji = null
        animateEmoji()
        animateGlow()
    }

    fun showGhostPrompt(emoji: String) {
        isActive = true
        ghostEmoji = emoji
        activeEmoji = null
        glowAlpha = 0f
        invalidate()
    }

    fun clearEffects() {
        isActive = false
        activeEmoji = null
        ghostEmoji = null
        glowAlpha = 0f
        emojiAnimator?.cancel()
        glowAnimator?.cancel()
        invalidate()
    }

    fun reset() {
        clearEffects()
        faces = emptyList()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isActive || faces.isEmpty() || imageWidth == 0 || imageHeight == 0) return

        // Mirror FaceOverlayView coordinate mapping
        val scaleX = width.toFloat() / imageHeight.toFloat()
        val scaleY = height.toFloat() / imageWidth.toFloat()

        val face = faces.firstOrNull() ?: return
        val bounds = face.boundingBox

        val left: Float
        val right: Float
        if (isFrontCamera) {
            left = width - bounds.right * scaleX
            right = width - bounds.left * scaleX
        } else {
            left = bounds.left * scaleX
            right = bounds.right * scaleX
        }
        val top = bounds.top * scaleY
        val bottom = bounds.bottom * scaleY

        val faceCx = (left + right) / 2f
        val faceCy = (top + bottom) / 2f
        val faceWidth = right - left
        val faceHeight = bottom - top

        // Draw glow ring around face
        if (glowAlpha > 0f) {
            val glowRadius = (faceWidth.coerceAtLeast(faceHeight) / 2f) * 1.3f
            glowPaint.color = glowColor
            glowPaint.alpha = (glowAlpha * 200).toInt()
            glowPaint.strokeWidth = 8f + glowAlpha * 8f
            canvas.drawCircle(faceCx, faceCy, glowRadius, glowPaint)
        }

        // Draw ghost prompt emoji (semi-transparent, above head)
        ghostEmoji?.let { emoji ->
            val ghostY = top - 20f
            canvas.drawText(emoji, faceCx, ghostY, ghostEmojiPaint)
        }

        // Draw active emoji (crown/sad) above head
        activeEmoji?.let { emoji ->
            emojiPaint.alpha = emojiAlpha
            val emojiY = top - 30f

            canvas.save()
            canvas.scale(emojiScale, emojiScale, faceCx, emojiY)
            canvas.drawText(emoji, faceCx, emojiY, emojiPaint)
            canvas.restore()
        }
    }

    private fun animateEmoji() {
        emojiAnimator?.cancel()
        emojiScale = 0.3f
        emojiAlpha = 255

        emojiAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val progress = it.animatedValue as Float
                emojiScale = 0.3f + 0.7f * progress.coerceAtMost(1f)
                // Fade out in the last 30%
                emojiAlpha = if (progress > 0.7f) {
                    ((1f - (progress - 0.7f) / 0.3f) * 255).toInt()
                } else {
                    255
                }
                invalidate()
            }
            // Keep emoji visible longer
            duration = 1000
            start()
        }
    }

    private fun animateGlow() {
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 800
            addUpdateListener {
                glowAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}
