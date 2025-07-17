package com.example.doan13.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan13.databinding.FragmentLikedPlaylistsTabBinding
import com.example.doan13.ui.adapters.PlaylistAdapter
import com.example.doan13.viewmodels.FavoriteViewModel
import com.google.firebase.auth.FirebaseAuth

class LikedPlaylistsTabFragment(private val favoriteViewModel: FavoriteViewModel) : Fragment() {
    private var _binding: FragmentLikedPlaylistsTabBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLikedPlaylistsTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
//        adapter = PlaylistAdapter({}, {}, favoriteViewModel) // Cần callback thực tế
//        binding.rvLikedPlaylists.layoutManager = LinearLayoutManager(context)
//        binding.rvLikedPlaylists.adapter = adapter

        // Giả định tải likedPlaylists (cần triển khai từ Firestore)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}