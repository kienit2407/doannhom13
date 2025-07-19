package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.databinding.ItemFavoritePlaylistBinding
import java.util.Locale

class FavoritePlaylistAdapter(
    private val onPlaylistClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<FavoritePlaylistAdapter.PlaylistViewHolder>(){
    private var playlists: List<PlaylistModel> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setPlaylists(playlists: List<PlaylistModel>) {
        this.playlists = playlists
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemFavoritePlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount(): Int = playlists.size

    inner class PlaylistViewHolder(private val binding: ItemFavoritePlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: PlaylistModel) {
            binding.txtNamPlaylist.text = playlist.name.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }}
            Glide.with(binding.root.context)
                .load(playlist.thumbnailUrl)
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(binding.imgPlaylist)
            binding.root.setOnClickListener {
                onPlaylistClick(playlist.playlistId)
            }
            binding.txtAmount.text = playlist.songIds.size.toString()

            val dateFormat = SimpleDateFormat("dd•MM•yyyy", Locale.getDefault())
            binding.txtDate.text = "CreateAt ${playlist?.createdAt?.let { dateFormat.format(it) } ?: "Chưa xác định"}"
            binding.imgButtonDelete.setOnClickListener {
                onDeleteClick(playlist.playlistId)
            }
        }
    }
}