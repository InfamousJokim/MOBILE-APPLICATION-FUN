package com.example.mobileappfun.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mobileappfun.R
import com.example.mobileappfun.databinding.FragmentCameraBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), HandGestureHelper.GestureListener {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetector
    private var handGestureHelper: HandGestureHelper? = null
    private var isFaceDetectionEnabled = true
    private var isGestureDetectionEnabled = false


    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            showPermissionUI()
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showGalleryBottomSheet()
        } else {
            Toast.makeText(requireContext(), R.string.storage_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupFaceDetector()
        setupGestureDetector()
        setupUI()
        checkCameraPermission()
    }

    private fun setupFaceDetector() {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        faceDetector = FaceDetection.getClient(options)
    }

    private fun setupGestureDetector() {
        try {
            handGestureHelper = HandGestureHelper(requireContext(), this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup gesture detector", e)
        }
    }

    private fun setupUI() {
        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        binding.switchCameraButton.setOnClickListener {
            switchCamera()
        }

        binding.galleryButton.setOnClickListener {
            openGallery()
        }

        binding.requestPermissionButton.setOnClickListener {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.faceDetectionToggle.setOnCheckedChangeListener { _, isChecked ->
            isFaceDetectionEnabled = isChecked
            if (!isChecked) {
                binding.faceOverlay.clearFaces()
                binding.faceCountText.visibility = View.GONE
            }
        }

        binding.gestureDetectionToggle.setOnCheckedChangeListener { _, isChecked ->
            isGestureDetectionEnabled = isChecked
            if (!isChecked) {
                binding.handOverlay.clear()
                binding.gestureText.visibility = View.GONE
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionUI() {
        binding.viewFinder.visibility = View.GONE
        binding.faceOverlay.visibility = View.GONE
        binding.handOverlay.visibility = View.GONE
        binding.controlsContainer.visibility = View.GONE
        binding.detectionToggles.visibility = View.GONE
        binding.faceCountText.visibility = View.GONE
        binding.gestureText.visibility = View.GONE
        binding.permissionMessage.visibility = View.VISIBLE
        binding.requestPermissionButton.visibility = View.VISIBLE
    }

    private fun hidePermissionUI() {
        binding.viewFinder.visibility = View.VISIBLE
        binding.faceOverlay.visibility = View.VISIBLE
        binding.handOverlay.visibility = View.VISIBLE
        binding.controlsContainer.visibility = View.VISIBLE
        binding.detectionToggles.visibility = View.VISIBLE
        binding.faceCountText.visibility = if (isFaceDetectionEnabled) View.VISIBLE else View.GONE
        binding.gestureText.visibility = View.GONE
        binding.permissionMessage.visibility = View.GONE
        binding.requestPermissionButton.visibility = View.GONE
    }

    private fun startCamera() {
        hidePermissionUI()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, CombinedAnalyzer())
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Toast.makeText(
                    requireContext(),
                    "Failed to start camera: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private inner class CombinedAnalyzer : ImageAnalysis.Analyzer {
        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

            if (isGestureDetectionEnabled && handGestureHelper != null) {
                handGestureHelper?.detectGestures(imageProxy, isFrontCamera)
            }

            if (isFaceDetectionEnabled) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    faceDetector.process(image)
                        .addOnSuccessListener { faces ->
                            processFaces(faces, imageProxy.width, imageProxy.height)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Face detection failed", e)
                        }
                        .addOnCompleteListener {
                            if (!isGestureDetectionEnabled) {
                                imageProxy.close()
                            }
                        }
                } else if (!isGestureDetectionEnabled) {
                    imageProxy.close()
                }
            } else if (!isGestureDetectionEnabled) {
                imageProxy.close()
            }

            if (isGestureDetectionEnabled) {
                imageProxy.close()
            }
        }
    }

    private fun processFaces(faces: List<Face>, imageWidth: Int, imageHeight: Int) {
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread

            val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
            binding.faceOverlay.setFaces(faces, imageWidth, imageHeight, isFrontCamera)

            if (faces.isNotEmpty()) {
                binding.faceCountText.text = getString(R.string.faces_detected, faces.size)
                binding.faceCountText.visibility = View.VISIBLE
            } else {
                binding.faceCountText.visibility = View.GONE
            }
        }
    }

    override fun onGestureResult(result: GestureRecognizerResult?, imageWidth: Int, imageHeight: Int) {
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread

            binding.handOverlay.setResults(result, imageWidth, imageHeight)

            if (result != null && result.gestures().isNotEmpty() && result.gestures()[0].isNotEmpty()) {
                val gesture = result.gestures()[0][0]
                val gestureName = HandGestureHelper.getGestureName(gesture.categoryName())
                if (gestureName.isNotEmpty()) {
                    binding.gestureText.text = "Gesture: $gestureName"
                    binding.gestureText.visibility = View.VISIBLE
                } else {
                    binding.gestureText.visibility = View.GONE
                }
            } else {
                binding.gestureText.visibility = View.GONE
            }
        }
    }

    override fun onGestureError(error: String) {
        Log.e(TAG, "Gesture error: $error")
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        binding.faceOverlay.clearFaces()
        binding.handOverlay.clear()
        startCamera()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MobileAppFun")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.photo_save_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.photo_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun openGallery() {
        checkStoragePermissionAndShowGallery()
    }

    private fun checkStoragePermissionAndShowGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                showGalleryBottomSheet()
            }
            else -> {
                requestStoragePermissionLauncher.launch(permission)
            }
        }
    }

    private fun showGalleryBottomSheet() {
        val bottomSheet = PhotoGalleryBottomSheet()
        bottomSheet.show(parentFragmentManager, PhotoGalleryBottomSheet.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        faceDetector.close()
        handGestureHelper?.close()
        _binding = null
    }

    companion object {
        private const val TAG = "CameraFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
