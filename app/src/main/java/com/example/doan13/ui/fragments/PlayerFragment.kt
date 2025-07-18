package com.example.doan13.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.databinding.DialogAddPlaylistBinding
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.FragmentFullPlayerBinding
import com.example.doan13.utilities.common.ToastCustom

import com.example.doan13.viewmodels.MediaViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class PlayerFragment : Fragment() {
    private var _binding: FragmentFullPlayerBinding? = null
    private val binding get() = _binding!!
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()
    private var rotateAnimation: RotateAnimation? = null
    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.GONE

        setupClickListeners()
        observeMediaState()
        startSeekBarUpdate()
        setupRotationAnimation()
    }

    private fun setupClickListeners() {

        binding.imgAddPlaylist.setOnClickListener {
            val songId = mediaViewModel.currentSong.value?.songId
            if(songId != null){
                showAddToPlaylistDialog(songId)
            }
        }

        binding.imgButtonDown.setOnClickListener {
            goBackToMiniPlayer()
        }


        binding.imgBtnPlay.setOnClickListener {
            mediaViewModel.togglePlayPause()
        }


        binding.imgbtnBack.setOnClickListener {
            mediaViewModel.playPrevious()
        }


        binding.imgbtnForward.setOnClickListener {
            mediaViewModel.playNext()
        }


        binding.imgbtnShuffle.setOnClickListener {
            mediaViewModel.toggleShuffle()
        }


        binding.imgbtnRepeat.setOnClickListener {
            mediaViewModel.toggleRepeatMode()
        }


        binding.skPlayer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = mediaViewModel.getDuration()
                    if (duration > 0) {
                        val newPosition = (duration * progress) /100
                        // ở đây nó sẽ tính toán vị trí mới
                        //ví dụ kéo tới tới 20% của bài hát thì là bao nhiêu giây
                        mediaViewModel.seekTo(newPosition)
                        updateTimeLabels(newPosition, duration)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun observeMediaState() {

        mediaViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            updateRotationAnimation(isPlaying)
        }

        mediaViewModel.currentSong.observe(viewLifecycleOwner) { song ->
            song?.let {
                binding.txtTitle.text = it.title.split(" ").joinToString(" ") {it.replaceFirstChar { it.uppercase() }}
                binding.txtArtist.text = it.artist.split(" ").joinToString(" ") {it.replaceFirstChar { it.uppercase() }}
                Glide.with(this)
                    .load(it.thumbnailUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(binding.imgThumbnails)
                Glide.with(this)
                    .load(it.thumbnailUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(binding.imgAvatar)

            }
        }


        mediaViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding.imgBtnPlay.setImageResource(
                if (isPlaying) R.drawable.pause_btn else R.drawable.play_btn
            )
        }


        mediaViewModel.isShuffled.observe(viewLifecycleOwner) { isShuffled ->
            binding.imgbtnShuffle.setColorFilter(
                if (isShuffled)
                    resources.getColor(R.color.primaryColor, null)
                else
                    resources.getColor(R.color.textColor2, null)
            )
        }


        mediaViewModel.repeatMode.observe(viewLifecycleOwner) { repeatMode ->
            val (icon, color) = when (repeatMode) {
                MediaViewModel.RepeatMode.OFF -> Pair(R.drawable.repeatoff, R.color.textColor2)
                MediaViewModel.RepeatMode.REPEAT_ALL -> Pair(R.drawable.repeat_on, R.color.primaryColor)
                MediaViewModel.RepeatMode.REPEAT_ONE -> Pair(R.drawable.repeat_one_track, R.color.primaryColor)
            }
            binding.imgbtnRepeat.setImageResource(icon)
            binding.imgbtnRepeat.setColorFilter(resources.getColor(color, null))
        }
    }

    private fun startSeekBarUpdate() {
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                val currentPosition = mediaViewModel.getCurrentPosition()
                val duration = mediaViewModel.getDuration()

                if (duration > 0) {
                    val progress = ((currentPosition * 100) / duration).toInt()
                    binding.skPlayer.progress = progress
                    updateTimeLabels(currentPosition, duration)
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }

    private fun updateTimeLabels(currentPosition: Long, duration: Long) {
        binding.txtCurrent.text = formatDuration(currentPosition)
        binding.txtTotal.text = formatDuration(duration)
    }

    private fun formatDuration(duration: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun goBackToMiniPlayer() {

        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
//        / Kiểm tra xem có fragment nào trong backStack không
        if (parentFragmentManager.backStackEntryCount > 0) {
            // Pop back stack để quay lại MiniPlayerFragment đã có sẵn
            parentFragmentManager.popBackStack()
        } else {
            // Nếu không có gì trong backStack, tạo MiniPlayerFragment mới
            parentFragmentManager.beginTransaction()
                .replace(R.id.miniPlayerContainer, MiniPlayerFragment())
                .commit()
        }
    }
    private fun setupRotationAnimation() {
        rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 10000
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator() // Đảm bảo chuyển động mượt mà
            fillAfter = true // Giữ trạng thái cuối cùng của animation
        }

    }
    private fun updateRotationAnimation(isPlaying: Boolean) {
        if (isPlaying) {
            if (binding.imgAvatar.animation == null) {
                binding.imgAvatar.startAnimation(rotateAnimation)

            }
        } else {
            binding.imgAvatar.clearAnimation()

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
        // Stop seekbar updates
        updateSeekBarRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Show bottom navigation when fragment is destroyed
        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
    }
}