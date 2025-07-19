package com.example.doan13.ui.fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan13.databinding.FragmentPlaylistsTabBinding
import com.example.doan13.ui.adapters.FavoritePlaylistAdapter
import com.example.doan13.ui.adapters.PlaylistAdapter
import com.example.doan13.utilities.common.ToastCustom
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PlaylistsTabFragment() : Fragment() {
    private var _binding: FragmentPlaylistsTabBinding? = null
    private val binding get() = _binding!!

    private val favoriteviewModel: FavoriteViewModel by activityViewModels ()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var adapter: FavoritePlaylistAdapter
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlaylistsTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setResetObserve()
        setRecycleView()
        setObserve()

        lifecycleScope.launch {
            try {
                favoriteviewModel.loadLikedPlaylists(userId)
            } catch (e: Exception) {
                Log.e("TracksTabFragment", "Error loading tracks: ${e.message}")
            }
        }

    }

    private fun setResetObserve() {
        favoriteviewModel.resetDeletePlaylistLikedResult()
    }

    private fun setRecycleView() {
        adapter = FavoritePlaylistAdapter(
            onPlaylistClick = { playlistId ->
                val action = ProfileFragmentDirections.actionProfileFragmentToPlaylistDetailFragment(playlistId)
                findNavController().navigate(action)
            },
            onDeleteClick = { playlistId ->
                showDialogdelete(playlistId, userId)
            }
        )
        binding.rvMyPlaylist.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyPlaylist.adapter = adapter
    }

    private fun setObserve() {
        favoriteviewModel.loading.observe(viewLifecycleOwner){isLoaded ->
            if (isLoaded){
                binding.progress.visibility =View.VISIBLE
            }
            else{
                binding.progress.visibility =View.GONE
            }
        }
        favoriteviewModel.playlistLiked.observe(viewLifecycleOwner) { playlists ->
            adapter.setPlaylists(playlists ?: emptyList())
            binding.rvMyPlaylist.visibility = if (playlists.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.textViewEmpty.visibility = if (playlists.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        favoriteviewModel.deletePlaylistLikedResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if(it.isSuccess) {
                    ToastCustom.showCustomToast(requireContext(),"Xoá playlist thành công")
                favoriteviewModel.loadLikedPlaylists(userId) // Cập nhật lại danh sách
            }
            }
        }
    }

    private fun showDialogdelete(playlistId: String, userId:String) {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.apply {
            //tiêu đề
            setTitle("Delete Playlist")
            setMessage("Do you want to delete this playlist?")
            //thêm nút phủ định và khẳng định
            setNegativeButton("No") { dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss() //bỏ qua khi nhấn no
            }
            //nust đồng ý
            setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
                favoriteviewModel.deletePlaylistLiked(playlistId, userId)
            }
            //ngăn khong cho đóng dialog khi click ra ngoài
        }
        dialog.show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}