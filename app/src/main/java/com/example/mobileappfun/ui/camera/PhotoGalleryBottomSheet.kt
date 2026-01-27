package com.example.mobileappfun.ui.camera

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mobileappfun.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.mobileappfun.databinding.BottomSheetGalleryBinding

class PhotoGalleryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoAdapter: PhotoGalleryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoAdapter = PhotoGalleryAdapter { uri ->
            openPhoto(uri)
        }

        binding.photosRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = photoAdapter
        }

        loadPhotos()
    }

    private fun loadPhotos() {
        val photos = mutableListOf<Uri>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(MediaStore.Images.Media._ID)

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Images.Media.DATA} LIKE ?"
        }

        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%Pictures/MobileAppFun%")
        } else {
            arrayOf("%MobileAppFun%")
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        requireContext().contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                photos.add(uri)
            }
        }

        if (photos.isEmpty()) {
            binding.emptyMessage.visibility = View.VISIBLE
        } else {
            binding.emptyMessage.visibility = View.GONE
        }

        photoAdapter.submitList(photos)
    }

    private fun openPhoto(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PhotoGalleryBottomSheet"
    }
}
