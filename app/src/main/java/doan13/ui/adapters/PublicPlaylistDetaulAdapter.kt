package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.databinding.ItemPlaylistDetailBinding
import com.example.doan13.databinding.ItemPublicPlaylistDetailBinding
import com.example.doan13.databinding.ItemPublicTracksBinding
import com.example.doan13.viewmodels.SongViewModel
import com.google.firebase.firestore.FirebaseFirestore

class PublicPlaylistDetaulAdapter(
    private val onSongClick: (String) -> Unit,
    private val onAddPLaylick :(String) -> Unit,
    private val songViewModel: SongViewModel
):RecyclerView.Adapter<PublicPlaylistDetaulAdapter.PublicPlaylistDetaulViewHolder>() {
    private var songs: List<SongModels> = emptyList()
    private val firestore = FirebaseFirestore.getInstance()
    @SuppressLint("NotifyDataSetChanged")
    fun setSongs(songs: List<SongModels>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicPlaylistDetaulViewHolder {
        val binding = ItemPublicTracksBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PublicPlaylistDetaulViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PublicPlaylistDetaulViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size

    inner class PublicPlaylistDetaulViewHolder(private val binding: ItemPublicTracksBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: SongModels) {
            //hiển thi tên bài hát
            binding.txtTitle.text = song.title
            //hiển thị thumbnail
            Glide.with(binding.root.context)
                .load(song.thumbnailUrl)
                .error(R.drawable.user)
                .placeholder(R.drawable.user)
                .into(binding.imgThumbnails )

           songViewModel.getUserName(song.userId){ getuserName ->
                binding.txtNameUploader.text = getuserName.toString()
            }
            binding.root.setOnClickListener {
                onSongClick(song.songId)
            }
            binding.imgbuttonAddPlaylist.setOnClickListener {
                onAddPLaylick(song.songId)
            }
            binding.txtLuotxem.text = song.playCount.toString()
        }
    }

}