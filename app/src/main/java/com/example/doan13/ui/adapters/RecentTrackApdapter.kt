package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.databinding.ItemPerTrackRecentBinding
import com.example.doan13.databinding.ItemPublicTracksBinding
import com.example.doan13.databinding.ItemRecentSongBinding
import com.example.doan13.viewmodels.SongViewModel
import com.google.firebase.firestore.FirebaseFirestore

class RecentTrackApdapter (
    //tạo biến contructor click
    private val onSongClick: (String) -> Unit,
    private val songViewModel: SongViewModel
): RecyclerView.Adapter<RecentTrackApdapter.RecentTrackViewHolder>(){
    private var songs: List<SongModels> = emptyList() // tạp playlist để xuất ra
    private val firestore = FirebaseFirestore.getInstance()
    @SuppressLint("NotifyDataSetChanged")
    fun setSongs(songs: List<SongModels>) {
        this.songs = songs
        notifyDataSetChanged() //lắn nghe thay đổi dũ liệu
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentTrackViewHolder {
        val binding = ItemPerTrackRecentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentTrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentTrackViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    inner class RecentTrackViewHolder(private val binding: ItemPerTrackRecentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: SongModels) {
            binding.txtTitle.text = song.title.split(" ").joinToString(" ") {it.replaceFirstChar { it.uppercase() }}
            Log.d("ThumbnailURL", song.thumbnailUrl)
            Glide.with(binding.root.context)
                .load(song.thumbnailUrl)
                .into(binding.imgThumbnails)
            binding.root.setOnClickListener {
                onSongClick(song.songId)
            }
            songViewModel.getUserName(song.userId){ userName->
                binding.txtArtist.text = userName.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }}

            }
            // Nút Thêm vào playlist//
        }
    }

    override fun getItemCount(): Int = songs.size


}