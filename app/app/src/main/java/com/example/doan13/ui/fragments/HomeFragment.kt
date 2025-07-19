package com.example.doan13.ui.fragments

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.databinding.DialogAddPlaylistBinding
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.FragmentHomeBinding
import com.example.doan13.ui.adapters.HomeAdapter
import com.example.doan13.utilities.common.ToastCustom

import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.MediaViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalTime

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapterMain: HomeAdapter
    private val authViewModel: AuthViewModel by activityViewModels()
    private val songViewModel: SongViewModel by activityViewModels()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels() // Thêm FavoriteViewModel
    private val mediaViewModel: MediaViewModel by activityViewModels()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE

        @RequiresApi(Build.VERSION_CODES.O)
        fun getGreeting(): String {
            val currentTime = LocalTime.now()
            return when {
                currentTime.isBefore(LocalTime.NOON) -> "Good Morning !👋🏻"
                currentTime.isBefore(LocalTime.of(18, 0)) -> "Good Afternoon !👋🏻"
                else -> "Good Night !👋🏻"
            }
        }
        Log.d("HomeFragment", "Loading data for userId: $userId")
        binding.txtDear.text = getGreeting()

        setRecycleView()
        setObserve()
        setOnClick()

        lifecycleScope.launch {
            try {
                //load data
                songViewModel.loadData(userId)
                authViewModel.loadUser(userId)
            } catch (e: Exception) {
                Log.e("TracksTabFragment", "Error loading tracks: ${e.message}")
            }
        }

    }

    private fun setOnClick() {
        binding.addButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_uploadFragment)

        }

        binding.imgCart.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_profile)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                try {
                    songViewModel.loadDataWipe(userId)
                } catch (e: Exception) {
                    Log.e("TracksTabFragment", "Error loading tracks: ${e.message}")
                }
            }
        }
    }

    private fun setObserve() {
        songViewModel.loading.observe(viewLifecycleOwner){isloading->
            if (isloading){
                binding.progressBar.visibility = View.VISIBLE
            }else{
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        authViewModel.userData.observe(viewLifecycleOwner) { data ->
            binding.txtName.text = data?.name?.split(" ")?.joinToString(" ") { it.replaceFirstChar { it.uppercase() } } ?: "Không xác định"
            if (data?.imageUrl != null) {
                Glide.with(this).load(data.imageUrl).into(binding.imgCart)
            } else {
                binding.imgCart.setImageResource(R.drawable.user)
            }
        }

        favoriteViewModel.createPlaylistResult.observe(viewLifecycleOwner) { result ->

            result?.let {
                when {
                    it.isSuccess -> {
//                        Toast.makeText(context, "", Toast.LENGTH_SHORT).show()
                        ToastCustom.showCustomToast(requireContext(), "Playlist created!")
                        favoriteViewModel.loadPlaylists(userId) // Cập nhật lại danh sách
                    }
                    it.isFailure -> Toast.makeText(context, "Error: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
                favoriteViewModel.resetCreatePlaylistResult()

            }
        }
        favoriteViewModel.addPLaylistToPlaylistResult.observe(viewLifecycleOwner){ result->
            result?.let {
                if(it.isSuccess){
                    favoriteViewModel.loadPlaylists(userId)
//                    Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT).show()
                    ToastCustom.showCustomToast(requireContext(), "Đã thêm vào playlist!")
                    favoriteViewModel.resetaddPlaylisttopLAYLISTResult()
                }
            }

        }

        lifecycleScope.launch {
            songViewModel.recentSongs.observe(viewLifecycleOwner) { updateAdapter() }
            songViewModel.artists.observe(viewLifecycleOwner) { updateAdapter() }
            songViewModel.newTracks.observe(viewLifecycleOwner) { updateAdapter() }
            songViewModel.popularPlaylists.observe(viewLifecycleOwner) { updateAdapter() }
            songViewModel.recommendedTracks.observe(viewLifecycleOwner) { updateAdapter() }
            songViewModel.recommendedPlaylists.observe(viewLifecycleOwner) { updateAdapter() }
        }
    }

    private fun setRecycleView() {
        //xử lí click
        adapterMain = HomeAdapter(
            onArtistClick = { artistId ->
                val action = HomeFragmentDirections.actionHomeFragmentToPublicProfileFragment(artistId)
                findNavController().navigate(action)
                activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.GONE
            },
            onSongClick = { songId ->
                // Phát bài hát cụ thể từ playlist
                authViewModel.updateRecentlyPlayed(userId, songId) // Gọi từ ViewModel
                mediaViewModel.setSongAndPlay(songId)
                favoriteViewModel.updatePlayCount(songId)
                showMiniPlayer()
            },
            onPlaylistClick = { playlistId ->
                val action = HomeFragmentDirections.actionHomeFragmentToPublicPlaylistDetailFragment(playlistId)
                findNavController().navigate(action)
            },
            onAddToPlaylistClick = {
                    songId -> showAddToPlaylistDialog(songId) },
            songViewModel = songViewModel,

            onAddToPlaylistUserClick = {playlistId->
                favoriteViewModel.addPlaylistToPlaylist(playlistId, userId)
            }
        )

        binding.rvHomeMain.layoutManager = LinearLayoutManager(context)
        binding.rvHomeMain.adapter = adapterMain
    }

    private fun updateAdapter() {
        val recentSongs = songViewModel.recentSongs.value.orEmpty()
        val artists = songViewModel.artists.value.orEmpty()
        val newTracks = songViewModel.newTracks.value.orEmpty()
        val popularPlaylists = songViewModel.popularPlaylists.value.orEmpty()
        val recommendedTracks = songViewModel.recommendedTracks.value.orEmpty()
        val recommendedPlaylists = songViewModel.recommendedPlaylists.value.orEmpty()

        if (recentSongs.isNotEmpty() || artists.isNotEmpty() || newTracks.isNotEmpty() || popularPlaylists.isNotEmpty() || recommendedTracks.isNotEmpty() || recommendedPlaylists.isNotEmpty()) {
            adapterMain.setData(recentSongs, artists, newTracks, popularPlaylists, recommendedTracks, recommendedPlaylists)
            Log.d("HomeFragment", "Update adapter: recentSongs=${recentSongs.size}, artists=${artists.size}, poular: ${popularPlaylists.size}")
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
                // Sử dụng layout custom
                val dialogBinding = DialogAddPlaylistBinding.inflate(layoutInflater)
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setView(dialogBinding.root)
                    .create()
            lifecycleScope.launch {
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
                    favoriteViewModel.message.observe(viewLifecycleOwner) { result ->
                        result?.let {
                            ToastCustom.showCustomToast(requireContext(), it)
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
                    ToastCustom.showCustomToast(requireContext(), "Please enter a playlist name!")
//                    Toast.makeText(context, "Please enter a playlist name", Toast.LENGTH_SHORT)
//                        .show()
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
