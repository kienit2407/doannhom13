package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.lifecycle.lifecycleScope
import com.example.doan13.R
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.data.repositories.AuthRepositories
import com.example.doan13.data.repositories.FavoriteRepositories
import com.example.doan13.databinding.ItemPlaylistDetailBinding
import com.example.doan13.viewmodels.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

class PlaylistDetailAdapter(
    private val onSongClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit = {}
) : RecyclerView.Adapter<PlaylistDetailAdapter.SongViewHolder>(){
    private var songs: List<SongModels> = emptyList()
    private val firestore = FirebaseFirestore.getInstance()
    @SuppressLint("NotifyDataSetChanged")
    fun setSongs(songs: List<SongModels>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemPlaylistDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size

    inner class SongViewHolder(private val binding: ItemPlaylistDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: SongModels) {
            //hiển thi tên bài hát
            binding.txtTitle.text = song.title
            //hiển thị thumbnail
            Glide.with(binding.root.context)
                .load(song.thumbnailUrl)
                .error(R.drawable.user)
                .placeholder(R.drawable.user)
                .into(binding.imgThumbnails )

             getUserName(song.userId){ getuserName ->
                    binding.txtNameUploader.text = getuserName.toString()
             }
            binding.root.setOnClickListener {
                onSongClick(song.songId)
            }

            binding.txtLuotxem.text = "${song.playCount} Lượt nghe"
            binding.imgbuttonDelete.setOnClickListener {
                onDeleteClick(song.songId)
            }
        }
    }
    private fun getUserName(userId: String, callback: (String) -> Unit) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("name") ?: "Unknown User"
                    callback(userName)
                } else {
                    callback("Unknown User")
                }
            }
            .addOnFailureListener {
                callback("Unknown User")
            }
    }
}