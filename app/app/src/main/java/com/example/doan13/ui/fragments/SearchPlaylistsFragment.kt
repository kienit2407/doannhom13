package com.example.doan13.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan13.R
import com.example.doan13.databinding.FragmentSearchPlaylistsBinding
import com.example.doan13.ui.adapters.PlaylistAdapter
import com.example.doan13.ui.adapters.PublicPLaylistAdapter
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.google.firebase.auth.FirebaseAuth

class SearchPlaylistsFragment : Fragment() {
    private var _binding: FragmentSearchPlaylistsBinding? = null
    private val binding get() = _binding!!
    private val songViewModel: SongViewModel by activityViewModels()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()
    private lateinit var adapter: PublicPLaylistAdapter
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "không có người dùng"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecycleView()
        setObserve()
    }

    private fun setObserve() {
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
        favoriteViewModel.addPLaylistToPlaylistResult.observe(viewLifecycleOwner){ result->
            result?.let {
                if(it.isSuccess){
                    favoriteViewModel.loadPlaylists(userId)
                    Toast.makeText(requireContext(), "Đã thêm vào playlist", Toast.LENGTH_SHORT).show()
                    favoriteViewModel.resetaddPlaylisttopLAYLISTResult()
                }
            }

        }
    }

    private fun setRecycleView() {
        adapter = PublicPLaylistAdapter(
            onPlaylistClick = { playlistId ->
                favoriteViewModel.updatePlayCountOfPlaylist(playlistId)
                val action = SearchFragmentDirections.actionSearchFragmentToPublicPlaylistDetailFragment(playlistId)
                findNavController().navigate(action)
            },
            onAddPlaylistClick = {playlistId->
                favoriteViewModel.addPlaylistToPlaylist(playlistId, userId)
            },
            songViewModel = songViewModel
        )
        binding.recyclerView.layoutManager = GridLayoutManager(
            binding.root.context,
            2,
            GridLayoutManager.VERTICAL,
            false
        )
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}