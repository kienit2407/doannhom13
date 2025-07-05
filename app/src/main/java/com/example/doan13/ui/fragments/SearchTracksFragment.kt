package com.example.doan13.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan13.R
import com.example.doan13.databinding.DialogAddPlaylistBinding
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.FragmentSearchTracksBinding
import com.example.doan13.ui.adapters.PublicTracksAdapter
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.MediaViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SearchTracksFragment : Fragment() {
    private var _binding: FragmentSearchTracksBinding? = null
    private val binding get() = _binding!!
    private val songViewModel: SongViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()
    private lateinit var publicTracksAdapter: PublicTracksAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchTracksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeSearchResults()
    }
    private fun setupRecyclerView() {
        publicTracksAdapter = PublicTracksAdapter(
            onSongClick = {songId ->
                val userId = authViewModel.getuserId()
                if (userId != null) {
                    authViewModel.updateRecentlyPlayed(userId, songId) // Gọi từ ViewModel
                }
                mediaViewModel.setSongAndPlay(songId)
                showMiniPlayer()
            },
            onAddToPlaylistClick = {songId ->
                showAddToPlaylistDialog(songId)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = publicTracksAdapter
        }
    }
    private fun observeSearchResults() {
        // Lắng nghe kết quả tìm kiếm từ ViewModel
        songViewModel.searchTracks.observe(viewLifecycleOwner) { tracks ->
            if (tracks.isEmpty()) {
                // Hiển thị thông báo không có kết quả
                binding.tvNoResults.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                // Hiển thị danh sách kết quả
                binding.tvNoResults.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                publicTracksAdapter.setSongs(tracks)
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

    private fun showAddToPlaylistDialog(songId: String) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        favoriteViewModel.loadPlaylists(userId)
        favoriteViewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            // Remove observer ngay sau khi nhận được data
            favoriteViewModel.playlists.removeObservers(viewLifecycleOwner) // xoá observe sau khi nhận được dữ liệu
            if (playlists.isNullOrEmpty()) {
                showCreatePlaylistDialog() // Hiển thị dialog tạo playlist nếu không có playlist
                return@observe
            }
            lifecycleScope.launch {
                // Sử dụng layout custom
                val dialogBinding = DialogAddPlaylistBinding.inflate(layoutInflater)
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setView(dialogBinding.root)
                    .create()

                // Điền danh sách playlist vào ListView (giả định có ListView trong XML)
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    playlists.map { it.name })
                dialogBinding.listPlaylists.adapter = adapter

                // Xử lý chọn playlist
                dialogBinding.listPlaylists.setOnItemClickListener { _, _, which, _ ->
                    val selectedPlaylistId = playlists[which].playlistId
                    favoriteViewModel.addSongToPlaylist(selectedPlaylistId, songId)
                    favoriteViewModel.addSongToPlaylistResult.observe(viewLifecycleOwner) { result ->
                        if (result?.isSuccess == true) {
                            Toast.makeText(context, "Đã thêm vào playlist!", Toast.LENGTH_SHORT)
                                .show()
                            favoriteViewModel.resetaddPlaylistResult()
                            favoriteViewModel.resetLoadPlaylist()
                            dialog.dismiss()
                        }
                    }
                }

                // Xử lý nút "Hủy" hoặc nút đóng (giả định có btnCancel trong XML)
                dialogBinding.imgButtonClose.setOnClickListener {
                    dialog.dismiss()
                }

                // Xử lý nút "Tạo Playlist mới" (giả định có btnCreateNew trong XML)
                dialogBinding.imgBtnAdd.setOnClickListener {
                    showCreatePlaylistDialog()
                    dialog.dismiss()
                }

                dialog.show()
            }
        }

    }

    private fun showCreatePlaylistDialog() {
        val dialogBinding = DialogCreatePlaylistBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()
        lifecycleScope.launch {
            dialogBinding.btnCreate.setOnClickListener {
                val name = dialogBinding.edtNamePlaylist.text.toString()
                if (name.isBlank()) {
                    Toast.makeText(context, "Please enter a playlist name", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    Toast.makeText(
                        context,
                        "Please log in to create a playlist",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                favoriteViewModel.createPlaylist(userId, name)
                dialog.dismiss()
            }

            dialogBinding.imgButtonClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}