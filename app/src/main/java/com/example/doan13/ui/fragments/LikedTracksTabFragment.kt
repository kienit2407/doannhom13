package com.example.doan13.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan13.databinding.FragmentLikedTracksTabBinding
import com.example.doan13.ui.adapters.SongAdapter
import com.example.doan13.viewmodels.FavoriteViewModel
import com.google.firebase.auth.FirebaseAuth

class LikedTracksTabFragment(private val favoriteViewModel: FavoriteViewModel) : Fragment() {
    private var _binding: FragmentLikedTracksTabBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SongAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLikedTracksTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
//        adapter = SongAdapter({}, {}, {}, favoriteViewModel) // Cần callback thực tế
//        binding.rvLikedTracks.layoutManager = LinearLayoutManager(context)
//        binding.rvLikedTracks.adapter = adapter

        // Giả định tải likedSongs (cần triển khai từ Firestore)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}