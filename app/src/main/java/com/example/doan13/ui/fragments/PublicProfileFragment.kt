package com.example.doan13.ui.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.example.doan13.R
import com.example.doan13.databinding.DialogAddPlaylistBinding
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.DialogModifyNameBinding
import com.example.doan13.databinding.FragmentProfileBinding
import com.example.doan13.databinding.FragmentProfileOtherBinding
import com.example.doan13.ui.activities.RegisterActivity
import com.example.doan13.ui.adapters.ProfileMyTrackApdapter
import com.example.doan13.ui.adapters.ProfileOtherTrackApdapter
import com.example.doan13.ui.adapters.ProfileTabAdapter
import com.example.doan13.utilities.common.ToastCustom
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.MediaViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


class PublicProfileFragment : Fragment() {
    private var _binding: FragmentProfileOtherBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val songViewModel: SongViewModel by activityViewModels()
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private val args: PublicProfileFragmentArgs by navArgs()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var adapter: ProfileOtherTrackApdapter // Sử dụng adapter đúng
    override fun onCreateView( // tạo view của fragment
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding = FragmentProfileOtherBinding.inflate(layoutInflater)
        return binding.root
    }


    //hàm xử lý trong đây

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Gọi dữ liệu
        lifecycleScope.launch {
            try {
                songViewModel.getUserAndUploadedSongs(args.userId)
            } catch (e: Exception) {
                Log.e("TracksTabFragment", "Error loading data: ${e.message}")
            }
        }
        lifecycleScope.launch {
            try {
                authViewModel.loadUserOther(args.userId)
            } catch (e: Exception) {
                Log.e("TracksTabFragment", "Error loading data: ${e.message}")
            }
        }
        setReset()
        setObserve()
        setRecycleView()
        setOnClick()

    }

    private fun setReset() {
        authViewModel.resetloadUserOther()
        songViewModel.resetuploaderSongs()
    }

    private fun setOnClick() {
        binding.btnBack.setOnClickListener{
            findNavController().popBackStack()
            activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
        }

    }


    private fun setRecycleView() {
        // Cấu hình RecyclerView
        adapter = ProfileOtherTrackApdapter (
            onSongClick = {songId ->
                authViewModel.updateRecentlyPlayed(userId, songId)
                favoriteViewModel.updatePlayCount(songId)
                mediaViewModel.setSongAndPlay(songId)
                showMiniPlayer()
            },
            onAddToPlaylistClick = { songID->
                showAddToPlaylistDialog(songId = songID )
            },
             songViewModel = songViewModel

        )
        binding.rvMyTrack.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyTrack.adapter = adapter
    }
    private fun setObserve() {
        songViewModel.uploaderSongs.observe(viewLifecycleOwner) { songs ->
            if (songs != null && songs.isNotEmpty()) {
                // Cập nhật adapter và UI
                adapter.setSongs(songs)
                binding.txtAmount.text = songs.size.toString()
                binding.textViewEmpty.visibility = View.GONE
            }
        }

        songViewModel.loading.observe(viewLifecycleOwner){isLoaded ->
            if (isLoaded){
                binding.progress.visibility =View.VISIBLE
            }
            else{
                binding.progress.visibility =View.GONE
            }
        }
        authViewModel.userDataOrther.observe(viewLifecycleOwner) { data ->
            data?.let {
                binding.txtName.text = data.name.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }} ?: "Không xác định"
                Glide.with(this)
                    .load(data.imageUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(binding.imgAvatar)
                binding.txtEmail.text = data.email
                binding.txtTrack.text = "Track Of ${data.name.split(" ").joinToString(" ") {it.replaceFirstChar {it.uppercase() }} ?: "Không xác định"}"
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
    }

}


