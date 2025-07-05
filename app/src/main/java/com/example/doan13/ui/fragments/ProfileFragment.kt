package com.example.doan13.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.databinding.DialogAddPlaylistBinding
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.DialogModifyAvartarBinding
import com.example.doan13.databinding.DialogModifyNameBinding
import com.example.doan13.databinding.FragmentProfileBinding
import com.example.doan13.ui.activities.RegisterActivity
import com.example.doan13.ui.adapters.ProfileMyTrackApdapter
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.MediaViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.example.doan13.viewmodels.UploadViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val songViewModel: SongViewModel by activityViewModels()
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private val uploadViewModel : UploadViewModel by activityViewModels ()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()

//    private lateinit var adapter: ProfileMyTrackApdapter // Sử dụng adapter đúng
private lateinit var adapter: ProfileMyTrackApdapter
    var currentSongId: String? = null
    private val selectAvatarLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val file = File(requireContext().cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }

                    if (file.exists() && file.canRead()) {
                         val userId = FirebaseAuth.getInstance().currentUser?.uid
                        userId?.let {  uploadViewModel.updateUserAvatar(file, it) }
                        // Hiển thị preview
                        Glide.with(this)
                            .load(file)
                            .circleCrop() // Làm tròn avatar
                            .into(binding.imgAvatar)
                    }
                } catch (e: Exception) {
                    Log.e("Fragment", "Error selecting avatar: ${e.message}")
                }
            }
        }
    }

    override fun onCreateView( // tạo view của fragment
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?:""


    //hàm xử lý trong đây
    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?:""
        authViewModel.initGoogle(requireContext())
        authViewModel.loadUser()
        //trong activity thì observe nó đã là viewlife rồi thì chỉ cần sử dụng this
        //do là fragment bị gỡ bỏ bị destroy nhưng vãn còn tỏng bộ nhớ làm app bị crash app
        //khi fragment destroy thì tự động dừng observe
        authViewModel.documentData.observe(viewLifecycleOwner) { data ->
            binding.txtName.text = data?.name ?: "Không xác định"
            Glide.with(this)
                .load(data?.imageUrl)
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(binding.imgAvatar)
            binding.txtTrack.text = "Track of ${data?.name}"
            binding.txtEmail.text = data?.email
        }

        songViewModel.isUploaded.observe(viewLifecycleOwner){ isUploaded ->
            if (isUploaded != null){
                if (isUploaded){
                    authViewModel.loadUser()
                    Toast.makeText(requireContext(), "Đã đổi ảnh đại diện", Toast.LENGTH_SHORT).show()
                    songViewModel.removeChangeAvt()
                }

            }
        }

// Chọn file thumbnail
        binding.imgAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            selectAvatarLauncher.launch(intent)
        }

        authViewModel.userInfo.observe(viewLifecycleOwner) { info ->
            if (info != null) {
                binding.txtName.text = info
            }
        }

        authViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.txtName.text = "Lỗi: $it"
            }
        }

        binding.btnSignOut.setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext())
            dialog.apply {
                //tiêu đề
                setTitle("Sign Out")
                setMessage("Do you want to sign out")
                //thêm nút phủ định và khẳng định
                setNegativeButton("No") { dialogInterface: DialogInterface, i: Int ->
                    dialogInterface.dismiss() //bỏ qua khi nhấn no
                }
                //nust đồng ý
                setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
                    authViewModel.signOut()

                }
                //ngăn khong cho đóng dialog khi click ra ngoài
            }
            dialog.show()
        }

        binding.ModifyName.setOnClickListener {
            showModifyDialog()
        }

        authViewModel.signOutSuccess.observe(viewLifecycleOwner) { success ->
            Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), RegisterActivity::class.java))
            mediaViewModel.pause()
            requireActivity().finish()
        }
        // Cấu hình RecyclerView
        adapter = ProfileMyTrackApdapter (
            onSongClick = {songId ->
                favoriteViewModel.updatePlayCount(songId)
                mediaViewModel.setSongAndPlay(songId)
                showMiniPlayer()
            },
            onDeleteSongClick = { songId->
              showDialogdelete(songId, userId)

            },
            onSAddPlaylist = { songId ->
                showAddToPlaylistDialog(songId )
            },
            songViewModel = songViewModel
        )
        binding.rvMyTrack.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyTrack.adapter = adapter

        // Gọi dữ liệu
        lifecycleScope.launch {
//
            try {
                songViewModel.getUserAndUploadedSongs(userId)
            } catch (e: Exception) {
                Log.e("TracksTabFragment", "Error loading data: ${e.message}")
            }
        }

        // Quan sát LiveData
        setupObservers()


    }
    private fun setupObservers() {

        uploadViewModel.uploadState.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { songId ->
                    authViewModel.loadUser()
                    Log.d("UploadFragment", "Upload success: $songId")
                    Toast.makeText(requireContext(), "Đã đổi ảnh", Toast.LENGTH_SHORT).show()
                    uploadViewModel.removeUploadState()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Lỗi: ${error.message}", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("UploadFragment", "Upload error: ${error.message}")
                }
            }
        }

        songViewModel.uploadedSongs.observe(viewLifecycleOwner) { songs ->
            if (songs != null && songs.isNotEmpty()) {
                adapter.setSongs(songs)
                binding.txtAmount.text = songs.size.toString()
            }else{
                binding.textViewEmpty.visibility = View.VISIBLE
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
        songViewModel.removeSongs.observe(viewLifecycleOwner){resurt ->
            if (resurt != null) {
                if(resurt.isSuccess){
                    songViewModel.getUserAndUploadedSongs(userId)
                    Toast.makeText(requireContext(), "Đã xoá bài hát", Toast.LENGTH_SHORT).show()
                    songViewModel.removeFunRemoveSong()
                }
            }
        }
//        // Cấu hình RecyclerView
//        adapter = ProfileMyTrackApdapter(onSongClick = { songId ->
//            // Phát bài hát cụ thể từ playlist
////            if (userId != null) {
////                authViewModel.updateRecentlyPlayed(userId, songId)
////            } // Gọi từ ViewModel
//            mediaViewModel.setSongAndPlay(songId)
//            showMiniPlayer()
//            // Xử lý khi click song (có thể để trống nếu chưa cần)
//        })
//        binding.rvMyTrackTab.layoutManager = LinearLayoutManager(requireContext())
//        binding.rvMyTrackTab.adapter = adapter
//        val userId = FirebaseAuth.getInstance().currentUser?.uid
//        if(userId != null){
//        authViewModel.getMyTracksByUserId(userId)
////        }
//        // Gọi dữ liệu
//        lifecycleScope.launch {
//            binding.progressBar.visibility = View.VISIBLE
//            try {
//                songViewModel.getUserAndUploadedSongs(userId)
//            } catch (e: Exception) {
//                Log.e("TracksTabFragment", "Error loading data: ${e.message}")
//            } finally {
//                binding.progressBar.visibility = View.GONE
//            }
//        }
//        if(userId != null){
////        authViewModel.getMyTracksByUserId(userId)
//        }
//        authViewModel.myTracks.observe(viewLifecycleOwner) { songs ->
//            adapter.setSongs(songs)
//            if (songs.isNullOrEmpty()) {
//                binding.rvMyTrackTab.visibility = View.GONE
//                binding.textViewEmpty.visibility = View.VISIBLE
//            } else {
//                binding.rvMyTrackTab.visibility = View.VISIBLE
//                binding.textViewEmpty.visibility = View.GONE
//                adapter.setSongs(songs) // Cập nhật adapter với dữ liệu
//            }
//        }
//        // Gọi và quan sát dữ liệu
//        lifecycleScope.launch {
//            binding.progressBar.visibility = View.VISIBLE
//            try {
//                val userId = FirebaseAuth.getInstance().currentUser?.uid
//                if (userId != null) {
//                    authViewModel.getMyTracksByUserId(userId)
//                }
//            } catch (e: Exception) {
//                Log.e("TracksTabFragment", "Error loading tracks: ${e.message}")
//            } finally {
//                binding.progressBar.visibility = View.GONE
//            }
//        }
        // Quan sát LiveData
//        setObserve()
//            setObserve()

//    private fun setObserve() {
//       authViewModel.myTracks.observe(viewLifecycleOwner) { songs ->
//            if (songs.isNullOrEmpty()) {
//                binding.rvMyTrackTab.visibility = View.GONE
//                binding.textViewEmpty.visibility = View.VISIBLE
//            } else {
//                binding.rvMyTrackTab.visibility = View.VISIBLE
//                binding.textViewEmpty.visibility = View.GONE
//                adapter.setSongs(songs) // Cập nhật adapter với dữ liệu
//            }
//        }
//    }
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
    private fun showDialogdelete(songId: String, userId:String) {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.apply {
            //tiêu đề
            setTitle("Delete Song")
            setMessage("Do you want to delete this ?")
            //thêm nút phủ định và khẳng định
            setNegativeButton("No") { dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss() //bỏ qua khi nhấn no
            }
            //nust đồng ý
            setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
                songViewModel.removeSong(songId, userId)

            }
            //ngăn khong cho đóng dialog khi click ra ngoài
        }
        dialog.show()
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
    private fun showModifyDialog() {
        val dialogBinding = DialogModifyNameBinding.inflate(layoutInflater)
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

            authViewModel.modifyName(userId, name)
            authViewModel.stateModifyName.observe(viewLifecycleOwner){result->
                if(result){
                    Toast.makeText(requireContext(), "Modify Success", Toast.LENGTH_SHORT).show()
                    authViewModel.loadUser()
                }
            }
            dialog.dismiss()
        }

        dialogBinding.imgButtonClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}


