package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.databinding.ItemPerTrackRecentBinding
import com.example.doan13.databinding.ItemPublicPlaylistDetailBinding
import com.example.doan13.databinding.ItemPublicTracksBinding
import com.example.doan13.databinding.ItemRecentSongBinding
import com.example.doan13.databinding.ItemTrackProfileMineBinding
import com.example.doan13.viewmodels.SongViewModel
import com.google.firebase.firestore.FirebaseFirestore

class ProfileOtherTrackApdapter (
    //tạo biến contructor click
    private val onSongClick: (String) -> Unit,
    private val onAddToPlaylistClick: (String) -> Unit,
    private val songViewModel: SongViewModel

): RecyclerView.Adapter<ProfileOtherTrackApdapter.ProfileMyTrackViewHolder>(){
    private var songs: List<SongModels> = emptyList() // tạp playlist để xuất ra
    private val firestore = FirebaseFirestore.getInstance()
    @SuppressLint("NotifyDataSetChanged")
    fun setSongs(songs: List<SongModels>) {
        this.songs = songs
        notifyDataSetChanged() //lắn nghe thay đổi dũ liệu
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileMyTrackViewHolder {
        val binding = ItemPublicTracksBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileMyTrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileMyTrackViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    inner class ProfileMyTrackViewHolder(private val binding: ItemPublicTracksBinding) : RecyclerView.ViewHolder(binding.root) {
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
            binding.imgbuttonAddPlaylist.setOnClickListener {
                onAddToPlaylistClick(song.songId)
            }


        }
    }

    override fun getItemCount(): Int = songs.size


}