package com.example.mobileappfun.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlin.math.hypot

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
        val recognizer = gestureRecognizer ?: return

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

        try {
            recognizer.recognizeAsync(mpImage, timestamp)
        } catch (e: IllegalArgumentException) {
            // MediaPipe requires strictly increasing timestamps in LIVE_STREAM mode.
            // On emulators, frames can arrive with duplicate or out-of-order timestamps.
            Log.w(TAG, "Skipped frame due to timestamp issue: ${e.message}")
        }
    }

    fun close() {
        gestureRecognizer?.close()
        gestureRecognizer = null
    }

    companion object {
        private const val TAG = "HandGestureHelper"
        const val KOREAN_HEART_CATEGORY = "Korean_Heart"
        const val FINGER_ON_MOUTH_CATEGORY = "Finger_On_Mouth"

        /**
         * Resolves the effective gesture category from a result.
         *
         * Priority order:
         *  1. Korean finger heart (landmark-based)
         *  2. Finger on mouth (index tip near face's mouth landmark)
         *  3. Model's own classification
         *
         * Pass mouthNormX/mouthNormY (normalized [0..1], mirrored for front camera)
         * from face detection. Pass -1 if no face is visible.
         */
        fun resolveCategory(
            result: GestureRecognizerResult?,
            mouthNormX: Float = -1f,
            mouthNormY: Float = -1f
        ): String? {
            if (result == null) return null
            val handLandmarks = result.landmarks()
            if (handLandmarks.isNotEmpty()) {
                val lm = handLandmarks[0]
                if (isKoreanHeart(lm)) return KOREAN_HEART_CATEGORY
                if (mouthNormX >= 0f && isFingerOnMouth(lm, mouthNormX, mouthNormY)) return FINGER_ON_MOUTH_CATEGORY
            }
            return if (result.gestures().isNotEmpty() && result.gestures()[0].isNotEmpty()) {
                result.gestures()[0][0].categoryName()
            } else null
        }

        /**
         * Detects the "finger on mouth" gesture using hand landmarks and the
         * face's mouth position.
         *
         * Conditions:
         *  1. Index finger is extended (tip further from wrist than MCP).
         *  2. Index tip is within 18% of normalized screen width from the mouth.
         *  3. Middle, ring, pinky are curled.
         *
         * Landmark indices:
         *   0=WRIST, 5=INDEX_MCP, 8=INDEX_TIP
         *   9=MIDDLE_MCP, 10=MIDDLE_PIP, 12=MIDDLE_TIP
         *   13=RING_MCP,  14=RING_PIP,   16=RING_TIP
         *   17=PINKY_MCP, 18=PINKY_PIP,  20=PINKY_TIP
         */
        private fun isFingerOnMouth(
            landmarks: List<NormalizedLandmark>,
            mouthNormX: Float,
            mouthNormY: Float
        ): Boolean {
            if (landmarks.size < 21) return false

            val wrist = landmarks[0]

            // Hand size: wrist → middle MCP
            val handSize = hypot(
                (wrist.x() - landmarks[9].x()).toDouble(),
                (wrist.y() - landmarks[9].y()).toDouble()
            ).toFloat()
            if (handSize < 0.01f) return false

            val indexTip = landmarks[8]
            val indexMcp = landmarks[5]

            // Index must be extended: tip further from wrist than MCP
            val tipToWrist = hypot(
                (indexTip.x() - wrist.x()).toDouble(),
                (indexTip.y() - wrist.y()).toDouble()
            ).toFloat()
            val mcpToWrist = hypot(
                (indexMcp.x() - wrist.x()).toDouble(),
                (indexMcp.y() - wrist.y()).toDouble()
            ).toFloat()
            if (tipToWrist < mcpToWrist * 1.6f) return false

            // Index tip must be close to the mouth (within 18% of screen)
            val distToMouth = hypot(
                (indexTip.x() - mouthNormX).toDouble(),
                (indexTip.y() - mouthNormY).toDouble()
            ).toFloat()
            if (distToMouth > 0.18f) return false

            // Middle, ring, pinky must be curled
            fun isCurled(tipIdx: Int, pipIdx: Int): Boolean {
                val t = hypot(
                    (landmarks[tipIdx].x() - wrist.x()).toDouble(),
                    (landmarks[tipIdx].y() - wrist.y()).toDouble()
                ).toFloat()
                val p = hypot(
                    (landmarks[pipIdx].x() - wrist.x()).toDouble(),
                    (landmarks[pipIdx].y() - wrist.y()).toDouble()
                ).toFloat()
                return t < p * 1.3f
            }

            return isCurled(12, 10) && isCurled(16, 14) && isCurled(20, 18)
        }

        /**
         * Detects the Korean finger heart (손하트) from hand landmarks.
         *
         * Conditions:
         *  1. Thumb tip (4) and index tip (8) are close together
         *     (distance < 40% of hand size).
         *  2. Middle, ring, and pinky fingers are curled — their tips are no
         *     further from the wrist than 1.3× their PIP joint distance.
         *
         * Landmark indices (MediaPipe 21-point hand model):
         *   0=WRIST, 4=THUMB_TIP, 8=INDEX_TIP
         *   9=MIDDLE_MCP, 10=MIDDLE_PIP, 12=MIDDLE_TIP
         *   13=RING_MCP,  14=RING_PIP,   16=RING_TIP
         *   17=PINKY_MCP, 18=PINKY_PIP,  20=PINKY_TIP
         */
        private fun isKoreanHeart(landmarks: List<NormalizedLandmark>): Boolean {
            if (landmarks.size < 21) return false

            val wrist = landmarks[0]

            // Hand size: wrist → middle finger MCP (scale reference)
            val handSize = hypot(
                (wrist.x() - landmarks[9].x()).toDouble(),
                (wrist.y() - landmarks[9].y()).toDouble()
            ).toFloat()
            if (handSize < 0.01f) return false

            // 1. Thumb tip and index tip must be close
            val tipDist = hypot(
                (landmarks[4].x() - landmarks[8].x()).toDouble(),
                (landmarks[4].y() - landmarks[8].y()).toDouble()
            ).toFloat()
            if (tipDist / handSize > 0.4f) return false

            // 2. Middle, ring, pinky must be curled
            fun isCurled(tipIdx: Int, pipIdx: Int): Boolean {
                val tipToWrist = hypot(
                    (landmarks[tipIdx].x() - wrist.x()).toDouble(),
                    (landmarks[tipIdx].y() - wrist.y()).toDouble()
                ).toFloat()
                val pipToWrist = hypot(
                    (landmarks[pipIdx].x() - wrist.x()).toDouble(),
                    (landmarks[pipIdx].y() - wrist.y()).toDouble()
                ).toFloat()
                return tipToWrist < pipToWrist * 1.3f
            }

            return isCurled(12, 10) && isCurled(16, 14) && isCurled(20, 18)
        }

        fun getGestureName(categoryName: String): String {
            return when (categoryName) {
                "Closed_Fist"       -> "Fist"
                "Open_Palm"         -> "Open Palm"
                "Pointing_Up"       -> "Pointing Up"
                "Thumb_Down"        -> "Thumbs Down"
                "Thumb_Up"          -> "Thumbs Up"
                "Victory"           -> "Peace Sign"
                "ILoveYou"          -> "I Love You"
                KOREAN_HEART_CATEGORY  -> "Korean Heart"
                FINGER_ON_MOUTH_CATEGORY -> "Finger On Mouth"
                "None"              -> ""
                else                -> categoryName
            }
        }
    }
}
