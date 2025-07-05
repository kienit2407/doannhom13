package com.example.doan13.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.databinding.FragmentMiniPlayerBinding
import com.example.doan13.viewmodels.MediaViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MiniPlayerFragment : Fragment() {
    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private var rotateAnimation: RotateAnimation? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMiniPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRotationAnimation()
        setupClickListeners()
        observeMediaState()
    }

    private fun setupRotationAnimation() {
        rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 10000 // 10 seconds per rotation
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator() // Đảm bảo chuyển động mượt mà
            fillAfter = true // Giữ trạng thái cuối cùng của animation
        }

    }

    private fun setupClickListeners() {
        // Play/Pause button
        binding.imgBtnPlay.setOnClickListener {
            mediaViewModel.togglePlayPause()
        }

        // Click anywhere on mini player to open full player
        binding.root.setOnClickListener {
            openFullPlayer()
        }
        binding.imaBtnClose.setOnClickListener{
            closeMiniPlayer()
        }
    }

    private fun observeMediaState() {
        // Observe current song
        mediaViewModel.currentSong.observe(viewLifecycleOwner) { song ->
            song?.let {
                binding.txtTitle.text = it.title
                binding.txtArtist.text = it.artist
                // Load album art
                Glide.with(this)
                    .load(it.thumbnailUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(binding.imgAvatar)
            }
        }

        // Observe playing state
        mediaViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            // Update play/pause button icon
            binding.imgBtnPlay.setImageResource(
                if (isPlaying) R.drawable.pause_btn else R.drawable.play_btn
            )

            // Start/stop rotation animation
            updateRotationAnimation(isPlaying)
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

    private fun openFullPlayer() {
        /// Kiểm tra xem PlayerFragment đã có trong container chưa
        val currentFragment = parentFragmentManager.findFragmentById(R.id.miniPlayerContainer)

        if (currentFragment !is PlayerFragment) {
            // Chỉ replace nếu fragment hiện tại không phải là PlayerFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.miniPlayerContainer, PlayerFragment())
                .addToBackStack("mini_to_full") // Đặt tên cho backStack entry
                .commit()
        }
        // Ẩn bottom navigation (nếu có)
        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.GONE
    }

    private fun closeMiniPlayer() {
        // Dừng phát nhạc khi tắt mini player
        mediaViewModel.setPlaying()
        parentFragmentManager.beginTransaction()
            .remove(this@MiniPlayerFragment)
            .commit()

        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.imgAvatar.clearAnimation()
        _binding = null
    }
}