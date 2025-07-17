package com.example.doan13.ui.fragments

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.databinding.DialogAddPlaylistBinding
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.FragmentPlaylistDetailBinding
import com.example.doan13.databinding.FragmentPublistPlaylistDetailBinding
import com.example.doan13.ui.adapters.PlaylistDetailAdapter
import com.example.doan13.ui.adapters.PublicPlaylistDetaulAdapter
import com.example.doan13.ui.fragments.HomeFragmentDirections
import com.example.doan13.utilities.common.ToastCustom
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.MediaViewModel
import com.example.doan13.viewmodels.PlaylistDetailViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class PublistPlaylistDetailFragment : Fragment() {
    private var _binding: FragmentPublistPlaylistDetailBinding? = null
    private val binding get() = _binding!!
    private val playlistDetailViewModel: PlaylistDetailViewModel by activityViewModels()
    private val args: PublistPlaylistDetailFragmentArgs by navArgs()
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private val songViewModel: SongViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    // Trạng thái shuffle cho playlist này
    private var isShuffleEnabled = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublistPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupObservers()

       lifecycleScope.launch {
           playlistDetailViewModel.loadSongInPlaylist(args.playlistId)
       }

    }

    private fun setupRecyclerView() {
        binding.rvTrack.layoutManager = LinearLayoutManager(context)

        val adapter = PublicPlaylistDetaulAdapter(
            onSongClick = { songId ->
                val userId = authViewModel.getuserId()
                if (userId != null) {
                    authViewModel.updateRecentlyPlayed(userId, songId) // Gọi từ ViewModel
                }
                // Phát bài hát cụ thể từ playlist
                playlistDetailViewModel.songs.value?.let { songs ->
                    mediaViewModel.setPlaylistAndPlay(songs, songs.indexOfFirst { it.songId == songId }, isShuffleEnabled)
                    showMiniPlayer()
                }
            },
            onAddPLaylick = {songId ->
                showAddToPlaylistDialog(songId)
            },
            songViewModel = songViewModel

        )
        binding.rvTrack.adapter = adapter
    }


    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Play All button
        binding.btnPlayALl.setOnClickListener {
            playAllSongs(shuffle = false)
            favoriteViewModel.updatePlayCountOfPlaylist(args.playlistId)
        }

        // Shuffle button
        binding.imgButtonShuffle.setOnClickListener {
            toggleShuffle()
            favoriteViewModel.updatePlayCountOfPlaylist(args.playlistId)
        }
        binding.LlWatchProfile.setOnClickListener {
            handleWatchProfile()
        }

    }

    private fun handleWatchProfile() {
        playlistDetailViewModel.playlist.observe(viewLifecycleOwner) { playlist ->
           playlist?.let {
               val action =
                   PublistPlaylistDetailFragmentDirections.actionPublicPlaylistDetailFragmentToProfile(
                       playlist.creatorId
                   )
               findNavController().navigate(action)
           }
        }
    }

    private fun setupObservers() {
        // Observe playlist data
        playlistDetailViewModel.playlist.observe(viewLifecycleOwner) { playlist ->
            playlist?.let {
                binding.txtNamePlaylist.text = it.name.split(" ").joinToString(" ") {it.replaceFirstChar { it.uppercase() }}
                Glide.with(this)
                    .load(it.thumbnailUrl ?: R.drawable.user)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(binding.imgPlaylist)


                binding.txtLuotxem.text = playlist.playCount.toString()
                val dateFormat = SimpleDateFormat("dd•MM•yyyy", Locale.getDefault())
                binding.txtDate.text = "Created ${it.createdAt?.let { date -> dateFormat.format(date) } ?: "Unknown"}"


                binding.txtmount.text = "${it.songIds.size} tracks"

                songViewModel.getUserName(playlist.creatorId){ userName->
                    binding.txtNameCreator.text = userName.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }}
                }
                songViewModel.getAvatarByUserId(playlist.creatorId){ avatar->
                    Glide.with(this)
                        .load(avatar)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .into(binding.imgAvatar)
                }
            }
        }
        playlistDetailViewModel.loading.observe(viewLifecycleOwner){isLoaded ->
            if (isLoaded){
                binding.progressBar.visibility =View.VISIBLE
            }
            else{
                binding.progressBar.visibility =View.GONE
            }
        }
        playlistDetailViewModel.songs.observe(viewLifecycleOwner) { songs ->
            if (songs.isNullOrEmpty()) {
                binding.rvTrack.visibility = View.GONE
                binding.textViewEmpty.visibility = View.VISIBLE
                // Disable play buttons when no songs
                binding.btnPlayALl.isEnabled = false
                binding.imgButtonShuffle.isEnabled = false
            } else {
                binding.rvTrack.visibility = View.VISIBLE
                binding.textViewEmpty.visibility = View.GONE
                binding.btnPlayALl.isEnabled = true
                binding.imgButtonShuffle.isEnabled = true

                // Update adapter
                (binding.rvTrack.adapter as? PublicPlaylistDetaulAdapter)?.setSongs(songs)
            }
        }
        favoriteViewModel.createPlaylistResult.observe(viewLifecycleOwner) { result ->

            result?.let {
                when {
                    it.isSuccess -> {
                        ToastCustom.showCustomToast(requireContext(), "Playlist created!")
                        favoriteViewModel.loadPlaylists(userId) // Cập nhật lại danh sách
                    }
                    it.isFailure -> Toast.makeText(context, "Error: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
                favoriteViewModel.resetCreatePlaylistResult()

            }
        }

    }

    private fun playAllSongs(shuffle: Boolean) {
        if (!shuffle){
            binding.imgButtonShuffle.setColorFilter(resources.getColor(R.color.textColor2, null))
        }
        if (shuffle){
            binding.imgButtonShuffle.setColorFilter(resources.getColor(R.color.primaryColor, null))
        }
        playlistDetailViewModel.songs.value?.let { songs ->
            if (songs.isNotEmpty()) {
                Log.d("PlaylistDetail", "Playing all songs. Shuffle: $shuffle, Songs count: ${songs.size}")

                // Set playlist and start playing
                mediaViewModel.setPlaylistAndPlay(songs, 0, shuffle)

                // Show mini player
                showMiniPlayer()
            } else {
                Toast.makeText(context, "No songs to play", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled

        // Update shuffle button appearance
        if (isShuffleEnabled) {
            binding.imgButtonShuffle.setColorFilter(resources.getColor(R.color.primaryColor, null))
            // Play with shuffle
            playAllSongs(shuffle = true)
        } else {
            binding.imgButtonShuffle.setColorFilter(resources.getColor(R.color.primaryColor, null))
            // If already playing, just disable shuffle in MediaViewModel
            if (mediaViewModel.isPlaying.value == true) {
                mediaViewModel.toggleShuffle() // This will disable shuffle if it's enabled
            }
        }

        Log.d("PlaylistDetail", "Shuffle toggled: $isShuffleEnabled")
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
        // Clean up observers
        playlistDetailViewModel.removeSongResult.removeObservers(viewLifecycleOwner)
    }
}