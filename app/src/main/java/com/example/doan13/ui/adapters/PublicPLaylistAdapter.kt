package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.databinding.ItemPerPlaylistBinding
import com.google.firebase.firestore.FirebaseFirestore

class PublicPLaylistAdapter (
    private val onPlaylistClick: (String) -> Unit,
) : RecyclerView.Adapter<PublicPLaylistAdapter.PlaylistViewHolder>(){
    private val firestore = FirebaseFirestore.getInstance()
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
                .load(playlist.thumbnailUrl )
                .error(R.drawable.user)
                .placeholder(R.drawable.user)
                .into(binding.imgThumbnails)

            getUserName(playlist.creatorId){userName->
                binding.txtArtist.text = userName.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }}
            }

            binding.root.setOnClickListener {
                onPlaylistClick(playlist.playlistId)
            }
            binding.txtLuotxem.text = playlist.playCount.toString()
            binding.txtmount.text = playlist.songIds.size.toString()


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