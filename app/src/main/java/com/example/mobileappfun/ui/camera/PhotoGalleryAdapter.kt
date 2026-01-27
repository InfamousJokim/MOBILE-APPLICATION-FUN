package com.example.mobileappfun.ui.camera

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mobileappfun.R
import com.example.mobileappfun.databinding.ItemPhotoBinding

class PhotoGalleryAdapter(
    private val onPhotoClick: (Uri) -> Unit
) : ListAdapter<Uri, PhotoGalleryAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            binding.photoImage.load(uri) {
                crossfade(true)
                placeholder(R.drawable.ic_photo_placeholder)
                error(R.drawable.ic_photo_placeholder)
            }

            binding.root.setOnClickListener {
                onPhotoClick(uri)
            }
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }
    }
}
