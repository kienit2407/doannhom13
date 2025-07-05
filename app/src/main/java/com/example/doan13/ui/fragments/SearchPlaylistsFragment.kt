package com.example.doan13.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan13.R
import com.example.doan13.databinding.FragmentSearchPlaylistsBinding
import com.example.doan13.ui.adapters.PlaylistAdapter
import com.example.doan13.ui.adapters.PublicPLaylistAdapter
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.SongViewModel

class SearchPlaylistsFragment : Fragment() {
    private var _binding: FragmentSearchPlaylistsBinding? = null
    private val binding get() = _binding!!
    private val songViewModel: SongViewModel by activityViewModels()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()
    private lateinit var adapter: PublicPLaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PublicPLaylistAdapter(
            onPlaylistClick = { playlistId ->
                favoriteViewModel.updatePlayCountOfPlaylist(playlistId)
                favoriteViewModel.updatePlayCountOfPlaylist(playlistId)
                val action = SearchFragmentDirections.actionSearchFragmentToPublicPlaylistDetailFragment(playlistId)
                findNavController().navigate(action)
            }
        )
        binding.recyclerView.layoutManager = GridLayoutManager(
            binding.root.context,
            2,
            GridLayoutManager.VERTICAL,
            false
        )
        binding.recyclerView.adapter = adapter

        songViewModel.searchPlaylists.observe(viewLifecycleOwner) { playlists ->
            if (playlists.isEmpty()) {
            binding.tvNoResults.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            // Hiển thị danh sách kết quả
            binding.tvNoResults.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            adapter.setPlaylists(playlists)
        }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}