package com.example.doan13.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.databinding.DialogModifyNameBinding
import com.example.doan13.databinding.DialogModifyPlaylistBinding
import com.example.doan13.databinding.FragmentPlaylistDetailBinding
import com.example.doan13.ui.adapters.PlaylistDetailAdapter
import com.example.doan13.utilities.common.ToastCustom
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.MediaViewModel
import com.example.doan13.viewmodels.PlaylistDetailViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File

class PlaylistDetailFragment : Fragment() {
    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!
    private val playlistDetailViewModel: PlaylistDetailViewModel by activityViewModels()
    private val args: PlaylistDetailFragmentArgs by navArgs()
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val songViewModel: SongViewModel by activityViewModels()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()
    private var thumbnailFile: File? = null
    var currentSongId: String? = null

    private var isShuffleEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupObservers()

        // Load playlist data
        lifecycleScope.launch {
            playlistDetailViewModel.loadSongInPlaylist(args.playlistId)
        }
    }

    private fun setupRecyclerView() {
        binding.rvTrack.layoutManager = LinearLayoutManager(context)//tạo adapter
        val adapter = PlaylistDetailAdapter(
            onSongClick = { songId ->
                favoriteViewModel.updatePlayCount(songId)
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
            onDeleteClick = { songId ->
                showDialogdelete(songId)
            }
        )
        binding.rvTrack.adapter = adapter //gán list cho adapgter
    }

    private fun showDialogdelete(songId: String) {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.apply {
            //tiêu đề
            setTitle("Delete Song")
            setMessage("Do you want to delete this song?")
            //thêm nút phủ định và khẳng định
            setNegativeButton("No") { dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss() //bỏ qua khi nhấn no
            }
            //nust đồng ý
            setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
                playlistDetailViewModel.removeSongFromPlaylist(args.playlistId, songId)
            }
            //ngăn khong cho đóng dialog khi click ra ngoài
        }
        dialog.show()
    }


    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Play All button
        binding.btnPlayALl.setOnClickListener {
            playAllSongs(shuffle = false)
        }

        // Shuffle button
        binding.imgButtonShuffle.setOnClickListener {
            toggleShuffle()
        }
        binding.ModifyName.setOnClickListener {
            showModifyNamePlaylistDialog()
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
            }
        }
        playlistDetailViewModel.songs.observe(viewLifecycleOwner) { songs ->
            if (songs.isNullOrEmpty()) {
                binding.rvTrack.visibility = View.GONE
                binding.textViewEmpty.visibility = View.VISIBLE

                binding.btnPlayALl.isEnabled = false
                binding.imgButtonShuffle.isEnabled = false
            } else {
                binding.rvTrack.visibility = View.VISIBLE
                binding.textViewEmpty.visibility = View.GONE
                binding.btnPlayALl.isEnabled = true
                binding.imgButtonShuffle.isEnabled = true
                // Update adapter
                (binding.rvTrack.adapter as? PlaylistDetailAdapter)?.setSongs(songs)
            }
        }
        // Observe remove song result
        playlistDetailViewModel.removeSongResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when {
                    it.isSuccess -> {
                        ToastCustom.showCustomToast(requireContext(),"Xoá bài hát thành công!")
                        // Reload playlist
                        playlistDetailViewModel.loadSongInPlaylist(args.playlistId)
                    }
                    it.isFailure -> {
                        Toast.makeText(context, "Error: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                playlistDetailViewModel.resetRemoveSongFromPlaylist()
            }
        }

        songViewModel.isUploadedThumnailPlaylist.observe(viewLifecycleOwner){state ->
            state?.let {
                if (it)
                    playlistDetailViewModel.loadSongInPlaylist(args.playlistId)
                    Toast.makeText(requireContext(), "Đã đổi ảnh playlist thành công", Toast.LENGTH_SHORT).show()
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
    private fun showModifyNamePlaylistDialog() {
        val dialogBinding = DialogModifyPlaylistBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDone.setOnClickListener {
            val name = dialogBinding.edtNewName.text.toString()
            if (name.isBlank()) {
                Toast.makeText(context, "Please enter new name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "Please log in to create a playlist", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                favoriteViewModel.modifyName(args.playlistId, name)
                favoriteViewModel.stateModifyName.observe(viewLifecycleOwner){result->
                    if(result){
                        playlistDetailViewModel.loadSongInPlaylist(args.playlistId)
                        Toast.makeText(requireContext(), "Modify Success", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
        }

        dialogBinding.imgButtonClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Clean up observers
        playlistDetailViewModel.removeSongResult.removeObservers(viewLifecycleOwner)
    }
}