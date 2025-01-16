package com.example.b2.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.b2.R
import com.example.b2.databinding.LayoutItemBinding
import com.example.b2.models.FileData

class FileAdapter(private var items: MutableList<FileData>) :
    RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = LayoutItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class FileViewHolder(private val binding: LayoutItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: FileData) {
            binding.tvName.text = file.name
            binding.tvPath.text = file.path
            binding.tvSize.text = formatSize(file.size.toLong())
            when (file.type) {
                "jpg", "jpeg", "png", "gif" -> binding.ivType.setImageResource(R.drawable.ic_image)
                "mp3", "wav", "flac", "aac" -> binding.ivType.setImageResource(R.drawable.ic_music)
                "mp4", "mkv", "mov", "avi" -> binding.ivType.setImageResource(R.drawable.ic_video)
                "pdf", "doc", "docx", "txt" -> binding.ivType.setImageResource(R.drawable.ic_document)
                else -> binding.ivType.setImageResource(R.drawable.ic_file)
            }
        }

        private fun formatSize(size: Long): String {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0

            return when {
                gb >= 1 -> String.format("%.2f GB", gb)
                mb >= 1 -> String.format("%.2f MB", mb)
                kb >= 1 -> String.format("%.2f KB", kb)
                else -> String.format("%d B", size)
            }
        }
    }
}