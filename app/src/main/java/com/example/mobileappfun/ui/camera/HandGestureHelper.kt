package com.example.mobileappfun.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

class HandGestureHelper(
    private val context: Context,
    private val listener: GestureListener
) {
    private var gestureRecognizer: GestureRecognizer? = null

    interface GestureListener {
        fun onGestureResult(result: GestureRecognizerResult?, imageWidth: Int, imageHeight: Int)
        fun onGestureError(error: String)
    }

    init {
        setupGestureRecognizer()
    }

    private fun setupGestureRecognizer() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath("gesture_recognizer.task")
                .build()

            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setNumHands(2)
                .setResultListener { result, _ ->
                    listener.onGestureResult(result, lastImageWidth, lastImageHeight)
                }
                .setErrorListener { error ->
                    listener.onGestureError(error.message ?: "Unknown error")
                }
                .build()

            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize gesture recognizer", e)
            listener.onGestureError("Failed to initialize: ${e.message}")
        }
    }

    private var lastImageWidth = 0
    private var lastImageHeight = 0

    fun detectGestures(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val bitmap = imageProxy.toBitmap()
        lastImageWidth = bitmap.width
        lastImageHeight = bitmap.height

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )

        lastImageWidth = rotatedBitmap.width
        lastImageHeight = rotatedBitmap.height

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        val timestamp = imageProxy.imageInfo.timestamp / 1000

        gestureRecognizer?.recognizeAsync(mpImage, timestamp)
    }

    fun close() {
        gestureRecognizer?.close()
        gestureRecognizer = null
    }

    companion object {
        private const val TAG = "HandGestureHelper"

        fun getGestureName(categoryName: String): String {
            return when (categoryName) {
                "Closed_Fist" -> "Fist"
                "Open_Palm" -> "Open Palm"
                "Pointing_Up" -> "Pointing Up"
                "Thumb_Down" -> "Thumbs Down"
                "Thumb_Up" -> "Thumbs Up"
                "Victory" -> "Peace Sign"
                "ILoveYou" -> "I Love You"
                "None" -> ""
                else -> categoryName
            }
        }
    }
}
