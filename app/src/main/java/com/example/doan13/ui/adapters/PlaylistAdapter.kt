package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.databinding.ItemPerPlaylistBinding
import com.example.doan13.viewmodels.SongViewModel
import com.google.firebase.firestore.FirebaseFirestore

class PlaylistAdapter(
    private val onPlaylistClick: (String) -> Unit,
    private val songViewModel: SongViewModel,
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {
    private var playlists: List<PlaylistModel> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setPlaylists(playlists: List<PlaylistModel>) {
        this.playlists = playlists
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPerPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount(): Int = playlists.size

    inner class PlaylistViewHolder(private val binding: ItemPerPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: PlaylistModel) {
            binding.txtTitle.text = playlist.name.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }}
            Glide.with(binding.root.context)
                .load(playlist.thumbnailUrl ?: "")
                .into(binding.imgThumbnails)

           songViewModel.getUserName(playlist.creatorId){userName->
                binding.txtArtist.text = userName.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }}
            }

            binding.root.setOnClickListener {
                onPlaylistClick(playlist.playlistId)
            }
            binding.txtLuotxem.text = playlist.playCount.toString()
            binding.txtmount.text = playlist.songIds.size.toString()
        }
    }
}