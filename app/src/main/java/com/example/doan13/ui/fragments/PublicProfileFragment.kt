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


    private lateinit var adapter: ProfileOtherTrackApdapter // Sử dụng adapter đúng
    override fun onCreateView( // tạo view của fragment
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding = FragmentProfileOtherBinding.inflate(layoutInflater)
        return binding.root
    }


    //hàm xử lý trong đây
    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authViewModel.initGoogle(requireContext())
        //trong activity thì observe nó đã là viewlife rồi thì chỉ cần sử dụng this
        //do là fragment bị gỡ bỏ bị destroy nhưng vãn còn tỏng bộ nhớ làm app bị crash app
        //khi fragment destroy thì tự động dừng observe

       songViewModel.userInfo.observe(viewLifecycleOwner) { data ->
            data?.let {
                binding.txtName.text = data?.name ?: "Không xác định"
                Glide.with(this)
                    .load(data?.imageUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(binding.imgAvatar)

                binding.txtEmail.text =data?.email
            }

        }
        binding.btnBack.setOnClickListener{
            findNavController().popBackStack()
            activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
        }
        // Gọi dữ liệu
        lifecycleScope.launch {
//            binding.progress.visibility = View.VISIBLE
            try {
                songViewModel.getUserAndUploadedSongs(args.userId)
            } catch (e: Exception) {
                Log.e("TracksTabFragment", "Error loading data: ${e.message}")
            } finally {
//                binding.progress.visibility = View.GONE
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

        setObsern()
        setRecycleView()

//        authViewModel.userInfo.observe(viewLifecycleOwner) { info ->
//            if (info != null) {
//                binding.txtName.text = info
//            }
//        }
//        authViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
//            error?.let {
//                binding.txtName.text = "Lỗi: $it"
//            }
//        }

//        binding.btnSignOut.setOnClickListener {
//            val dialog = AlertDialog.Builder(requireContext())
//            dialog.apply {
//                //tiêu đề
//                setTitle("Sign Out")
//                setMessage("Do you want to sign out")
//                //thêm nút phủ định và khẳng định
//                setNegativeButton("No") { dialogInterface: DialogInterface, i: Int ->
//                    dialogInterface.dismiss() //bỏ qua khi nhấn no
//                }
//                //nust đồng ý
//                setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
//                    authViewModel.signOut()
//                }
//                //ngăn khong cho đóng dialog khi click ra ngoài
//            }
//            dialog.show()
//        }
//
//        binding.ModifyName.setOnClickListener {
//            showCreatePlaylistDialog()
//        }
//        authViewModel.signOutSuccess.observe (viewLifecycleOwner) {success->
//            Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show()
//            startActivity(Intent(requireContext() , RegisterActivity::class.java))
//        }
//        // Cấu hình RecyclerView
//        adapter = ProfileMyTrackApdapter(onSongClick = { songId ->
//            // Phát bài hát cụ thể từ playlist
//
//                authViewModel.updateRecentlyPlayed(args.userId, songId)
//
//            mediaViewModel.setSongAndPlay(songId)
//            showMiniPlayer()
//            // Xử lý khi click song (có thể để trống nếu chưa cần)
//        })
//        binding.rvMyTrackTab.layoutManager = LinearLayoutManager(requireContext())
//        binding.rvMyTrackTab.adapter = adapter

        // Gọi và quan sát dữ liệu
//        lifecycleScope.launch {
//            try {
//                songViewModel.getMyTracks(args.userId)
//            } catch (e: Exception) {
//                Log.e("TracksTabFragment", "Error loading tracks: ${e.message}")
//            }
//        }
        // Quan sát LiveData
//        setObserve()


    }

    @SuppressLint("SuspiciousIndentation")
    private fun setRecycleView() {
        // Cấu hình RecyclerView

        adapter = ProfileOtherTrackApdapter (
            onSongClick = {songId ->
              val userId = authViewModel.getuserId()
                userId?.let { authViewModel.updateRecentlyPlayed(userId, songId) }
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
    private fun setObsern() {
        songViewModel.uploadedSongs.observe(viewLifecycleOwner) { songs ->
            Log.d("YourFragment", "Observed uploadedSongs: ${songs?.size}")
            if (songs != null && songs.isNotEmpty()) {
                songViewModel.removeObSong()
                // Cập nhật adapter và UI
                adapter.setSongs(songs)
                binding.txtAmount.text = songs.size.toString()
//                binding.textViewEmpty.visibility = View.GONE
            } else {
                // Hiển thị thông báo rỗng
//                binding.textViewEmpty.visibility = View.VISIBLE
            }
        }
    }

    //    private fun setObserve() {
//        songViewModel.getMyTracks.observe(viewLifecycleOwner) { songs ->
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


