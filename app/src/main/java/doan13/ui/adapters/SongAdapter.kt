package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.text.capitalize
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.databinding.ItemPerSongBinding
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SongAdapter (
    private val onSongClick: (String) -> Unit,
    private val onAddToPlaylistClick: (String) -> Unit,
    private val songViewModel: SongViewModel
) :RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    private var songs: List<SongModels> = emptyList()
    private val firestore = FirebaseFirestore.getInstance()
    @SuppressLint("NotifyDataSetChanged")
    fun setSongs(songs: List<SongModels>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemPerSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size

    inner class SongViewHolder(private val binding: ItemPerSongBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: SongModels) {
            binding.txtTitle.text = song.title.split(" ").joinToString(" ") {it.replaceFirstChar { it.uppercase() }}
            Log.d("ThumbnailURL", song.thumbnailUrl)
            Glide.with(binding.root.context)
                .load(song.thumbnailUrl)
                .into(binding.imgThumbnails)
            binding.txtArtist.text = song.artist.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }}
            binding.txtLuotxem.text = "${song.playCount} Lượt nghe"
            binding.root.setOnClickListener {
                onSongClick(song.songId)
            }
           songViewModel.getUserName(song.userId){userName->
                binding.txtNameUploader.text = userName.toString()
            }
            // Nút Thêm vào playlist
            binding.imgbuttonAddPlaylist.setOnClickListener {
                onAddToPlaylistClick(song.songId)
            }

//
        }
    }

}