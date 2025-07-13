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
import com.example.doan13.R
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.FragmentFavoriteSongBinding
import com.example.doan13.ui.adapters.FavoritePlaylistAdapter
import com.example.doan13.ui.adapters.PlaylistAdapter
import com.example.doan13.utilities.common.ToastCustom
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FavoriteSongFragment : Fragment() {
    private var _binding: FragmentFavoriteSongBinding? = null
    private val binding get() = _binding!!
    private val favoriteviewModel: FavoriteViewModel by activityViewModels ()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var adapter: FavoritePlaylistAdapter

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteSongBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //load dữ liệu
        lifecycleScope.launch {
            try {
                favoriteviewModel.loadPlaylists(userId)
            } catch (e: Exception) {
                Log.e("TracksTabFragment", "Error loading tracks: ${e.message}")
            }
        }
        setOnClick()
        setObserve()
        setRecycleView()
    }

    private fun setOnClick() {
        binding.root.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun setObserve() {
        favoriteviewModel.loading.observe(viewLifecycleOwner){isLoaded ->
            if (isLoaded){
                binding.progressBar.visibility =View.VISIBLE
            }
            else{
                binding.progressBar.visibility =View.GONE
            }
        }

        favoriteviewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            adapter.setPlaylists(playlists ?: emptyList())
            binding.txtYourPlaylist.text = "Your Playlist (${playlists?.size ?: 0})"
            binding.textViewEmpty.visibility = if (playlists.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        favoriteviewModel.createPlaylistResult.observe(viewLifecycleOwner) { result ->

            result?.let {
                when {
                    it.isSuccess -> {
                        ToastCustom.showCustomToast(requireContext(), "Playlist created!")
                        favoriteviewModel.loadPlaylists(userId) // Cập nhật lại danh sách
                    }
                    it.isFailure -> Toast.makeText(context, "Error: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
                favoriteviewModel.resetCreatePlaylistResult()

            }
        }

        favoriteviewModel.deletePlaylistResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when {
                    it.isSuccess -> {
                        favoriteviewModel.loadPlaylists(userId) // Cập nhật lại danh sách
                        ToastCustom.showCustomToast(requireContext(), "Playlist deleted!")
                    }
                    it.isFailure -> Toast.makeText(context, "Error: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()

                }
                favoriteviewModel.resetDeletePlaylistResult()

            }
        }
    }

    private fun setRecycleView() {
        binding.rvPlaylists.layoutManager = LinearLayoutManager(context)
        adapter = FavoritePlaylistAdapter(
            onPlaylistClick = { playlistId ->
                val action = FavoriteSongFragmentDirections.actionFavoriteSongFragmentToPlaylistDetailFragment(playlistId)
                findNavController().navigate(action)
            },
            onDeleteClick = { playlistId ->
                showDialogdelete(playlistId, userId)
            }
        )
        binding.rvPlaylists.adapter = adapter
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
                favoriteviewModel.deletePlaylist(playlistId, userId)

            }
            //ngăn khong cho đóng dialog khi click ra ngoài
        }
        dialog.show()
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
//                    Toast.makeText(context, "", Toast.LENGTH_SHORT).show()
                    ToastCustom.showCustomToast(requireContext(), "Please enter a playlist name!")
                    return@setOnClickListener
                }

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    Toast.makeText(context, "Please log in to create a playlist", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                favoriteviewModel.createPlaylist(userId, name)
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