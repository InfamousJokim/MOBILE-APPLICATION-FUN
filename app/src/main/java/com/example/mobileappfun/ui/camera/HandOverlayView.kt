package com.example.mobileappfun.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

class HandOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var result: GestureRecognizerResult? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    private val landmarkPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        strokeWidth = 8f
    }

    private val connectionPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val gesturePaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val gestureBackgroundPaint = Paint().apply {
        color = Color.argb(180, 76, 175, 80)
        style = Paint.Style.FILL
    }

    fun setResults(result: GestureRecognizerResult?, imageWidth: Int, imageHeight: Int) {
        this.result = result
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
    }

    fun clear() {
        this.result = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val result = this.result ?: return
        if (imageWidth == 0 || imageHeight == 0) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        result.landmarks().forEachIndexed { handIndex, landmarks ->
            // Draw connections
            HAND_CONNECTIONS.forEach { (start, end) ->
                if (start < landmarks.size && end < landmarks.size) {
                    val startLandmark = landmarks[start]
                    val endLandmark = landmarks[end]

                    canvas.drawLine(
                        startLandmark.x() * width,
                        startLandmark.y() * height,
                        endLandmark.x() * width,
                        endLandmark.y() * height,
                        connectionPaint
                    )
                }
            }

            // Draw landmarks
            landmarks.forEach { landmark ->
                canvas.drawCircle(
                    landmark.x() * width,
                    landmark.y() * height,
                    8f,
                    landmarkPaint
                )
            }

            // Draw gesture label
            if (handIndex < result.gestures().size && result.gestures()[handIndex].isNotEmpty()) {
                val gesture = result.gestures()[handIndex][0]
                val gestureName = HandGestureHelper.getGestureName(gesture.categoryName())

                if (gestureName.isNotEmpty()) {
                    val wristLandmark = landmarks[0]
                    val x = wristLandmark.x() * width
                    val y = wristLandmark.y() * height - 60

                    val textWidth = gesturePaint.measureText(gestureName)
                    val padding = 16f

                    canvas.drawRoundRect(
                        x - textWidth / 2 - padding,
                        y - 40,
                        x + textWidth / 2 + padding,
                        y + 10,
                        12f, 12f,
                        gestureBackgroundPaint
                    )

                    canvas.drawText(gestureName, x, y, gesturePaint)
                }
            }
        }
    }

    companion object {
        private val HAND_CONNECTIONS = listOf(
            Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),
            Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),
            Pair(0, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),
            Pair(0, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),
            Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),
            Pair(5, 9), Pair(9, 13), Pair(13, 17)
        )
    }
}
