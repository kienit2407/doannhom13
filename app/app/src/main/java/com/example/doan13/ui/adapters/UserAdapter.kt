package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.data.models.auth.UserModel
import com.example.doan13.databinding.ItemPerArtistBinding
import com.example.doan13.databinding.ItemPerSongBinding

class UserAdapter (
    private val onArtistClick: (String) -> Unit
): RecyclerView.Adapter<UserAdapter.ArtistViewHolder>() {
    private var artists: List<UserModel> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setArtists(artists: List<UserModel>) {
        this.artists = artists
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemPerArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.bind(artists[position])
    }

    override fun getItemCount(): Int = artists.size //lấy hết

    inner class ArtistViewHolder(private val binding: ItemPerArtistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(artist: UserModel) {
            Glide.with(binding.root.context)
                .load(artist.imageUrl ?: R.drawable.user)
                .circleCrop()
                .into(binding.imgArtist)
            binding.txtNameArtist.text = artist.name.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }}

            binding.root.setOnClickListener {
                onArtistClick(artist.uid) //nhấn vào id của user nào đó
            }
        }
    }
}