package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.databinding.ItemPlaylistDetailBinding
import com.example.doan13.databinding.ItemPublicTracksBinding
import com.google.firebase.firestore.FirebaseFirestore

class PublicTracksAdapter (
    //tạo biến contructor click
    private val onSongClick: (String) -> Unit,
    private val onAddToPlaylistClick: (String) -> Unit,
) : RecyclerView.Adapter<PublicTracksAdapter.PublicTracksViewHolder>() { //kế thừa revycleview adaptwe
    private var songs: List<SongModels> = emptyList() // tạp playlist để xuất ra
    private val firestore = FirebaseFirestore.getInstance()
    @SuppressLint("NotifyDataSetChanged")
    fun setSongs(songs: List<SongModels>) {
        this.songs = songs
        notifyDataSetChanged() //lắn nghe thay đổi dũ liệu
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicTracksViewHolder {
        val binding = ItemPublicTracksBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PublicTracksViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PublicTracksViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    inner class PublicTracksViewHolder(private val binding: ItemPublicTracksBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: SongModels) {
            binding.txtTitle.text = song.title.split(" ").joinToString(" ") {it.replaceFirstChar { it.uppercase() }}
            Log.d("ThumbnailURL", song.thumbnailUrl)
            Glide.with(binding.root.context)
                .load(song.thumbnailUrl)
                .into(binding.imgThumbnails)
            binding.txtArtist.text = song.artist.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }}
            binding.txtLuotxem.text = song.playCount.toString()

            binding.root.setOnClickListener {
                onSongClick(song.songId)
            }
            getUserName(song.userId){userName->
                binding.txtNameUploader.text = userName
            }
            // Nút Thêm vào playlist
            binding.imgbuttonAddPlaylist.setOnClickListener {
                onAddToPlaylistClick(song.songId)
            }
        }
    }

    override fun getItemCount(): Int = songs.size

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