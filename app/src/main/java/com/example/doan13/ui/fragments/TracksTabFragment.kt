package com.example.doan13.ui.fragments

import android.app.AlertDialog
import android.content.DialogInterface
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
import com.example.doan13.databinding.BottomSheetCustomBinding
import com.example.doan13.databinding.DialogAddPlaylistBinding
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.FragmentTracksTabBinding
import com.example.doan13.ui.adapters.PlaylistDetailAdapter
import com.example.doan13.ui.adapters.ProfileMyTrackApdapter
import com.example.doan13.ui.adapters.PublicTracksAdapter
import com.example.doan13.ui.adapters.SongAdapter
import com.example.doan13.utilities.common.ToastCustom
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.MediaViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.example.doan13.viewmodels.UploadViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TracksTabFragment() : Fragment() {
    private var _binding: FragmentTracksTabBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val songViewModel: SongViewModel by activityViewModels()
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private val uploadViewModel : UploadViewModel by activityViewModels ()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()

    private lateinit var adapter: ProfileMyTrackApdapter // Sử dụng adapter đúng
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTracksTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Quan sát LiveData
        setResetObserve()
        setupObservers()
        setRecycleView()

    }

    private fun setResetObserve() {
        songViewModel.resetremoveSongs()
    }


    private fun setRecycleView() {
        // Cấu hình RecyclerView
        adapter = ProfileMyTrackApdapter (
            onSongClick = {songId ->
                favoriteViewModel.updatePlayCount(songId)
                mediaViewModel.setSongAndPlay(songId)
                showMiniPlayer()
            },
            onOptionSongClick = { songId->
               viewLifecycleOwner.lifecycleScope.launch {
                    songViewModel.loadPublicStatusForTrack(songId)
                }
                showBottomSheetOption(songId)

            },
            songViewModel = songViewModel
        )
        binding.rvMyTrack.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyTrack.adapter = adapter

    }

    private fun setupObservers() {
        songViewModel.uploaderSongs.observe(viewLifecycleOwner) { songs ->
                adapter.setSongs(songs ?: emptyList())
                binding.rvMyTrack.visibility = if (songs.isNullOrEmpty()) View.GONE else View.VISIBLE
                binding.textViewEmpty.visibility = if (songs.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        songViewModel.loading.observe(viewLifecycleOwner){isLoaded ->
            if (isLoaded){
                binding.progress.visibility = View.VISIBLE
            }
            else{
                binding.progress.visibility =View.GONE
            }
        }
        songViewModel.removeSongs.observe(viewLifecycleOwner){result ->
            if (result != null) {
                if(result.isSuccess){
                    songViewModel.getUserAndUploadedSongs(userId)
                    Toast.makeText(requireContext(), "Đã xoá bài hát", Toast.LENGTH_SHORT).show()
                }
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
    private fun showBottomSheetOption (songId: String){

        val bottomBinding = BottomSheetCustomBinding.inflate(layoutInflater)
       val bottomSheet = BottomSheetDialog(requireContext())

        //quan sát dữ liệu để cập nhật trạng thái cho switch
        songViewModel.isPublic.observe(viewLifecycleOwner){ state->
            bottomBinding.switchIsPublic.isChecked = state
        }

        bottomSheet.setContentView(bottomBinding.root)

        bottomBinding.imgButtonClose.setOnClickListener {
            bottomSheet.dismiss()
        }
        bottomBinding.btnOptionDelete.setOnClickListener {
            showDialogdelete(songId, userId)
            bottomSheet.dismiss()
        }
        bottomBinding.btnOptionAddplaylist.setOnClickListener {
            showAddToPlaylistDialog(songId)
            bottomSheet.dismiss()
        }
        bottomBinding.switchIsPublic.setOnClickListener {
           val isChecked = bottomBinding.switchIsPublic.isChecked
            songViewModel.updatePublicStatus(songId, isChecked)
            if(isChecked){
                ToastCustom.showCustomToast(requireContext(),"Đã bật công khai")
            }else{
                ToastCustom.showCustomToast(requireContext(),"Đã tắt công khai")
            }
        }
        songViewModel.removeSongs.observe(viewLifecycleOwner){result ->
            result?.let {
                if(it.isSuccess){
                    bottomSheet.dismiss()
                }
            }
        }
        bottomSheet.show()
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
}