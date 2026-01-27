package com.example.mobileappfun.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var faces: List<Face> = emptyList()
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var isFrontCamera: Boolean = false

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val landmarkPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.argb(150, 0, 0, 0)
        style = Paint.Style.FILL
    }

    fun setFaces(faces: List<Face>, imageWidth: Int, imageHeight: Int, isFrontCamera: Boolean) {
        this.faces = faces
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.isFrontCamera = isFrontCamera
        invalidate()
    }

    fun clearFaces() {
        this.faces = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (faces.isEmpty() || imageWidth == 0 || imageHeight == 0) return

        val scaleX = width.toFloat() / imageHeight.toFloat()
        val scaleY = height.toFloat() / imageWidth.toFloat()

        for (face in faces) {
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

            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, boxPaint)

            drawLandmarks(canvas, face, scaleX, scaleY)
            drawFaceInfo(canvas, face, left, top)
        }
    }

    private fun drawLandmarks(canvas: Canvas, face: Face, scaleX: Float, scaleY: Float) {
        val landmarks = listOf(
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.NOSE_BASE,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.MOUTH_BOTTOM,
            FaceLandmark.LEFT_EAR,
            FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.RIGHT_CHEEK
        )

        for (landmarkType in landmarks) {
            face.getLandmark(landmarkType)?.let { landmark ->
                val x = if (isFrontCamera) {
                    width - landmark.position.x * scaleX
                } else {
                    landmark.position.x * scaleX
                }
                val y = landmark.position.y * scaleY
                canvas.drawCircle(x, y, 8f, landmarkPaint)
            }
        }
    }

    private fun drawFaceInfo(canvas: Canvas, face: Face, left: Float, top: Float) {
        val info = buildString {
            face.smilingProbability?.let {
                if (it > 0.5f) append("Smiling ")
            }
            face.leftEyeOpenProbability?.let { leftEye ->
                face.rightEyeOpenProbability?.let { rightEye ->
                    if (leftEye < 0.3f && rightEye < 0.3f) {
                        append("Eyes Closed ")
                    }
                }
            }
            face.headEulerAngleY.let { angle ->
                when {
                    angle > 20 -> append("Looking Left ")
                    angle < -20 -> append("Looking Right ")
                }
            }
        }

        if (info.isNotEmpty()) {
            val textWidth = textPaint.measureText(info)
            val textHeight = textPaint.textSize
            val padding = 8f

            canvas.drawRect(
                left,
                top - textHeight - padding * 2,
                left + textWidth + padding * 2,
                top,
                textBackgroundPaint
            )
            canvas.drawText(info, left + padding, top - padding, textPaint)
        }
    }
}
