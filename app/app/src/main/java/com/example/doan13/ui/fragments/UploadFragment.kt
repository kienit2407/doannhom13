package com.example.doan13.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.DialogWaitUploadBinding
import com.example.doan13.databinding.FragmentUploadBinding
import com.example.doan13.utilities.common.ToastCustom
import com.example.doan13.viewmodels.SongViewModel
import com.example.doan13.viewmodels.UploadViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class UploadFragment : Fragment() {
    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!
    private val uploadViewModel : UploadViewModel by activityViewModels()
    private var thumbnailFile: File? = null
    private var mp3File: File? = null

    private val selectThumbnailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val file = File(requireContext().cacheDir, "thumbnail_${System.currentTimeMillis()}.jpg")
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (file.exists() && file.canRead()) {
                        thumbnailFile = file
                        binding.textViewThumbnailStatus.text = "Đã chọn: ${file.name}"
                        binding.imgIcon.visibility = View.GONE
                        Glide.with(this)
                            .load(file)
                            .into(binding.imgThumbnails)
                        Log.d("UploadFragment", "Thumbnail selected: ${file.absolutePath}")
                    } else {
                        binding.textViewThumbnailStatus.text = "File thumbnail không hợp lệ"
                    }
                } catch (e: Exception) {
                    binding.textViewThumbnailStatus.text = "Lỗi khi chọn thumbnail: ${e.message}"
                }
            }
        }
    }

    private val selectMp3Launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val file = File(requireContext().cacheDir, "song_${System.currentTimeMillis()}.mp3")
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (file.exists() && file.canRead()) {
                        mp3File = file
                        binding.textViewMp3Status.text = "Đã chọn: ${file.name}"
                        Log.d("UploadFragment", "MP3 selected: ${file.absolutePath}")
                    } else {
                        binding.textViewMp3Status.text = "File MP3 không hợp lệ"
                        Log.e("UploadFragment", "Invalid MP3 file")
                    }
                } catch (e: Exception) {
                    binding.textViewMp3Status.text = "Lỗi khi chọn MP3: ${e.message}"
                    Log.e("UploadFragment", "Error selecting MP3: ${e.message}")
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.GONE
        setOnClick()

    }

    private fun setOnClick() {
        // Chọn file thumbnail
        binding.imgThumbnails.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            selectThumbnailLauncher.launch(intent)
        }

        // Chọn file MP3
        binding.btnSelectMp3.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/mpeg"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            selectMp3Launcher.launch(intent)
        }

        // Tải lên bài hát
        binding.buttonUpload.setOnClickListener {
            val title = binding.edtTitle.text.toString()
            val artist = binding.edtArtist.text.toString()
            val uploaderId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            if (title.isBlank() || artist.isBlank() || thumbnailFile == null || mp3File == null || uploaderId.isBlank()) { //isBlank là kiểm tra có rỗng hay không
                binding.textViewStatus.text = "Vui lòng nhập đầy đủ thông tin và chọn file"
                Log.e("UploadFragment", "Missing info: title=$title, artist=$artist, thumbnail=$thumbnailFile, mp3=$mp3File, uploaderId=$uploaderId")
                return@setOnClickListener
            }
            lifecycleScope.launch {
                uploadViewModel.uploadSong(thumbnailFile!!, mp3File!!, title, artist, uploaderId)
            }
            showUploadingStateDialog()
        }
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showUploadingStateDialog() {
        val dialogBinding = DialogWaitUploadBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.setCancelable(false)
        // Quan sát tiến trình tải lên
        uploadViewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
          progress?.let {
              dialogBinding.progressBar.progress = progress
              dialogBinding.txtProcessState.text = "Đang tải $progress%"
              Log.d("UploadFragment", "Upload progress: $progress%")
          }
        }
        // Quan sát trạng thái tải lên
        uploadViewModel.uploadState.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { songId ->
                    ToastCustom.showCustomToast(requireContext(), "Tải lên thành công!")
                    dialog.dismiss()
                    uploadViewModel.removeUploadState()
                    lifecycleScope.launch {
                        delay(1000)
                        findNavController().popBackStack()
                    }
                }.onFailure { error ->
                    Log.e("UploadFragment", "Upload error: ${error.message}")
                }
            }
        }
        lifecycleScope.launch {
            dialogBinding.btnCancel.setOnClickListener {
                uploadViewModel.cancelUpload()
                dialog.dismiss()
                ToastCustom.showCustomToast(requireContext(), "Đã huỷ tải lên")
                uploadViewModel.uploadSongSession = null
            }
            dialog.show()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}