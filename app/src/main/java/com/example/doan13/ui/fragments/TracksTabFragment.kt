package com.example.doan13.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan13.R
import com.example.doan13.databinding.FragmentTracksTabBinding
import com.example.doan13.ui.adapters.PlaylistDetailAdapter
import com.example.doan13.ui.adapters.ProfileMyTrackApdapter
import com.example.doan13.ui.adapters.PublicTracksAdapter
import com.example.doan13.ui.adapters.SongAdapter
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.MediaViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class TracksTabFragment(private val favoriteViewModel: FavoriteViewModel) : Fragment() {
    private var _binding: FragmentTracksTabBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val songViewModel: SongViewModel by activityViewModels()
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private lateinit var adapter: ProfileMyTrackApdapter // Sử dụng adapter đúng

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTracksTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Cấu hình RecyclerView
//        adapter = ProfileMyTrackApdapter(onSongClick = { songId ->
//            // Phát bài hát cụ thể từ playlist
//            authViewModel.updateRecentlyPlayed(userId, songId) // Gọi từ ViewModel
//            mediaViewModel.setSongAndPlay(songId)
//            showMiniPlayer()
//            // Xử lý khi click song (có thể để trống nếu chưa cần)
//        })
        binding.rvMyTrackTab.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyTrackTab.adapter = adapter

        // Gọi và quan sát dữ liệu
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                songViewModel.getMyTracks(userId) // Gọi hàm suspend
            } catch (e: Exception) {
                Log.e("TracksTabFragment", "Error loading tracks: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
        // Quan sát LiveData
        setObserve()
    }

    private fun setObserve() {
        songViewModel.getMyTracks.observe(viewLifecycleOwner) { songs ->
            if (songs.isNullOrEmpty()) {
                binding.rvMyTrackTab.visibility = View.GONE
                binding.textViewEmpty.visibility = View.VISIBLE
            } else {
                binding.rvMyTrackTab.visibility = View.VISIBLE
                binding.textViewEmpty.visibility = View.GONE
                adapter.setSongs(songs) // Cập nhật adapter với dữ liệu
            }
        }
    }
    private fun showMiniPlayer() {
        lifecycleScope.launch {
            // Show mini player in the mini player container
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.miniPlayerContainer, MiniPlayerFragment())
                .commit()

            Log.d("PlaylistDetail", "Mini player shown")
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}