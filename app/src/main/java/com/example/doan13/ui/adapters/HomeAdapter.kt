package com.example.doan13.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.data.models.auth.UserModel
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.databinding.ItemArtistBinding
import com.example.doan13.databinding.ItemNewTrackBinding
import com.example.doan13.databinding.ItemPopularPlaylistBinding
import com.example.doan13.databinding.ItemRecentSongBinding
import com.example.doan13.databinding.ItemRecommendedPlaylistsBinding
import com.example.doan13.databinding.ItemRecommendedTracksBinding
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.SongViewModel

class HomeAdapter(
    private val onArtistClick: (String) -> Unit, //vào cá nhân
    private val onSongClick: (String) -> Unit, //bấm vào bài hát
    private val onPlaylistClick: (String) -> Unit, //bấm vào vào playlist
    private val onAddToPlaylistClick: (String) -> Unit, //bấm vào thêm playlist
    private val songViewModel: SongViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_RECENT_SONGS = 0
        const val TYPE_ARTISTS = 1
        const val TYPE_NEW_TRACKS = 2
        const val TYPE_POPULAR_PLAYLISTS = 3
        const val TYPE_RECOMMENDED_TRACKS = 4
        const val TYPE_RECOMMENDED_PLAYLISTS = 5
    }

    private val items = mutableListOf<Int>()
    private var recentSongs: List<SongModels>? = null
    private var artists: List<UserModel>? = null
    private var newTracks: List<SongModels>? = null
    private var popularPlaylists: List<PlaylistModel>? = null
    private var recommendedTracks: List<SongModels>? = null
    private var recommendedPlaylists: List<PlaylistModel>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setData(
        recentSongs: List<SongModels>?,
        artists: List<UserModel>?,
        newTracks: List<SongModels>?,
        popularPlaylists: List<PlaylistModel>?,
        recommendedTracks: List<SongModels>?,
        recommendedPlaylists: List<PlaylistModel>?
    ) {
        this.recentSongs = recentSongs
        this.artists = artists
        this.newTracks = newTracks
        this.popularPlaylists = popularPlaylists
        this.recommendedTracks = recommendedTracks
        this.recommendedPlaylists = recommendedPlaylists

        items.clear()
        if (!recentSongs.isNullOrEmpty()) items.add(TYPE_RECENT_SONGS)
        if (!artists.isNullOrEmpty()) items.add(TYPE_ARTISTS)
        if (!newTracks.isNullOrEmpty()) items.add(TYPE_NEW_TRACKS)
        if (!popularPlaylists.isNullOrEmpty()) items.add(TYPE_POPULAR_PLAYLISTS)
        if (!recommendedTracks.isNullOrEmpty()) items.add(TYPE_RECOMMENDED_TRACKS)
        if (!recommendedPlaylists.isNullOrEmpty()) items.add(TYPE_RECOMMENDED_PLAYLISTS)

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RECENT_SONGS -> {
                val binding = ItemRecentSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                RecentSongsViewHolder(binding, onSongClick, onAddToPlaylistClick, songViewModel)
            }
            TYPE_ARTISTS -> {
                val binding = ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ArtistsViewHolder(binding, onArtistClick)
            }
            TYPE_NEW_TRACKS -> {
                val binding = ItemNewTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                NewTracksViewHolder(binding, onSongClick, onAddToPlaylistClick, songViewModel)
            }
            TYPE_POPULAR_PLAYLISTS -> {
                val binding = ItemPopularPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PopularPlaylistsViewHolder(binding, onPlaylistClick, songViewModel)
            }
            TYPE_RECOMMENDED_TRACKS -> {
                val binding = ItemRecommendedTracksBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                RecommendedTracksViewHolder(binding, onSongClick, onAddToPlaylistClick, songViewModel)
            }
            TYPE_RECOMMENDED_PLAYLISTS -> {
                val binding = ItemRecommendedPlaylistsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                RecommendedPlaylistsViewHolder(binding, onPlaylistClick, songViewModel)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RecentSongsViewHolder -> holder.bind(recentSongs ?: emptyList())
            is ArtistsViewHolder -> holder.bind(artists ?: emptyList())
            is NewTracksViewHolder -> holder.bind(newTracks ?: emptyList())
            is PopularPlaylistsViewHolder -> holder.bind(popularPlaylists ?: emptyList())
            is RecommendedTracksViewHolder -> holder.bind(recommendedTracks ?: emptyList())
            is RecommendedPlaylistsViewHolder -> holder.bind(recommendedPlaylists ?: emptyList())
        }
    }

    override fun getItemCount(): Int = items.size

    class RecentSongsViewHolder(
        private val binding: ItemRecentSongBinding,
        private val onSongClick: (String) -> Unit,
        private val onAddToPlaylistClick: (String) -> Unit,
        private val songViewModel: SongViewModel // Thêm favoriteViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(songs: List<SongModels>) {
            val adapter = RecentTrackApdapter(onSongClick, songViewModel)
            binding.rvRecentSongs.layoutManager =  LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvRecentSongs.adapter = adapter
            adapter.setSongs(songs)
        }
    }

    class ArtistsViewHolder(
        private val binding: ItemArtistBinding,
        private val onArtistClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(artists: List<UserModel>) {
            val adapter = UserAdapter(onArtistClick)
            binding.rvArtists.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvArtists.adapter = adapter
            adapter.setArtists(artists)
        }
    }

    class NewTracksViewHolder(
        private val binding: ItemNewTrackBinding,
        private val onSongClick: (String) -> Unit,
        private val onAddToPlaylistClick: (String) -> Unit,
        private val songViewModel: SongViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(songs: List<SongModels>) {
            val adapter = SongAdapter(onSongClick, onAddToPlaylistClick, songViewModel)
            binding.rvNewTrack.layoutManager = GridLayoutManager(
                binding.root.context,
                2,
                GridLayoutManager.HORIZONTAL,
                false
            )
            binding.rvNewTrack.adapter = adapter
            adapter.setSongs(songs)
        }
    }

    class PopularPlaylistsViewHolder(
        private val binding: ItemPopularPlaylistBinding,
        private val onPlaylistClick: (String) -> Unit,
        private val songViewModel: SongViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlists: List<PlaylistModel>) {
            val adapter = PlaylistAdapter(onPlaylistClick, songViewModel =songViewModel )
            binding.rvPopularPlaylists.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvPopularPlaylists.adapter = adapter
            adapter.setPlaylists(playlists)
        }
    }

    class RecommendedTracksViewHolder(
        private val binding: ItemRecommendedTracksBinding,
        private val onSongClick: (String) -> Unit,
        private val onAddToPlaylistClick: (String) -> Unit,
        private val songViewModel: SongViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(songs: List<SongModels>) {
            val adapter = SongAdapter(onSongClick, onAddToPlaylistClick, songViewModel)
            binding.rvRecommendedTrack.layoutManager = GridLayoutManager(
                binding.root.context,
                2,
                GridLayoutManager.HORIZONTAL,
                false
            )
            binding.rvRecommendedTrack.adapter = adapter
            adapter.setSongs(songs)
        }
    }

    class RecommendedPlaylistsViewHolder(
        private val binding: ItemRecommendedPlaylistsBinding,
        private val onPlaylistClick: (String) -> Unit,
       private val songViewModel: SongViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlists: List<PlaylistModel>) {
            val adapter = PlaylistAdapter(onPlaylistClick, songViewModel)
            binding.rvRecommendedPlaylist.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvRecommendedPlaylist.adapter = adapter
            adapter.setPlaylists(playlists)
        }
    }
}