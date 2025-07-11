package com.example.doan13.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.doan13.R
import com.example.doan13.databinding.DialogAddPlaylistBinding
import com.example.doan13.databinding.DialogCreatePlaylistBinding
import com.example.doan13.databinding.DialogModifyAvartarBinding
import com.example.doan13.databinding.DialogModifyNameBinding
import com.example.doan13.databinding.FragmentProfileBinding
import com.example.doan13.databinding.ToastCustomBinding
import com.example.doan13.ui.activities.RegisterActivity
import com.example.doan13.ui.adapters.ProfileMyTrackApdapter
import com.example.doan13.ui.adapters.ProfileTabAdapter
import com.example.doan13.ui.adapters.SearchPagerAdapter
import com.example.doan13.utilities.common.ToastCustom
import com.example.doan13.viewmodels.AuthViewModel
import com.example.doan13.viewmodels.FavoriteViewModel
import com.example.doan13.viewmodels.MediaViewModel
import com.example.doan13.viewmodels.SongViewModel
import com.example.doan13.viewmodels.UploadViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.getValue


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val songViewModel: SongViewModel by activityViewModels()
    private val mediaViewModel: MediaViewModel by activityViewModels()
    private val uploadViewModel : UploadViewModel by activityViewModels ()
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()

    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?:""
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //load user
        lifecycleScope.launch {
            authViewModel.loadUser(userId)
        }
        lifecycleScope.launch {
            try {
                favoriteViewModel.loadLikedPlaylists(userId)
            } catch (e: Exception) {
                Log.e("TracksTabFragment", "Error loading tracks: ${e.message}")
            }
        }
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
        setTabAdapter()
        setOnClick()
    }

    private fun setOnClick() {

        binding.imgAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            selectAvatarLauncher.launch(intent)
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
    }

    private fun setTabAdapter() {
        val pagerAdapter = ProfileTabAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Tracks"    // Tab bài hát
                1 -> "Playlists" // Tab playlist
                else -> ""
            }
        }.attach()
    }

    private fun setupObservers() {

        uploadViewModel.updateAvtState.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { songId ->
                    authViewModel.loadUser(userId)
                    Log.d("UploadFragment", "Upload success: $songId")
                    ToastCustom.showCustomToast(requireContext(), "Đã đổi ảnh")
                    uploadViewModel.removeUploadState()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Lỗi: ${error.message}", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("UploadFragment", "Upload error: ${error.message}")
                }
            }
        }

        favoriteViewModel.playlistLiked.observe(viewLifecycleOwner) { playlists ->
            binding.txtAmountPlaylist.text = playlists?.size.toString()
        }
        songViewModel.uploaderSongs.observe(viewLifecycleOwner) { songs ->
            if (songs != null && songs.isNotEmpty()) {
                binding.txtAmount.text = songs.size.toString()
            }
        }

        //trong activity thì observe nó đã là viewlife rồi thì chỉ cần sử dụng this
        //do là fragment bị gỡ bỏ bị destroy nhưng vãn còn tỏng bộ nhớ làm app bị crash app
        //khi fragment destroy thì tự động dừng observe
        authViewModel.userData.observe(viewLifecycleOwner) { data ->
            binding.txtName.text = data?.name?.split(" ")?.joinToString(" ") {it.replaceFirstChar {it.uppercase() }} ?: "Không xác định"
            Glide.with(this)
                .load(data?.imageUrl)
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(binding.imgAvatar)
            binding.txtEmail.text = data?.email
        }

        uploadViewModel.loading.observe(viewLifecycleOwner){ isLoaded ->
            if (isLoaded){
                binding.progressBar.visibility = View.VISIBLE
            }
            else{
                binding.progressBar.visibility =View.GONE
            }
        }
        authViewModel.signOutSuccess.observe(viewLifecycleOwner) { success ->
            ToastCustom.showCustomToast(requireContext(), "Đăng xuất thành công !")
            startActivity(Intent(requireContext(), RegisterActivity::class.java))
            mediaViewModel.pause()
            requireActivity().finish()
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
            authViewModel.modifyName(userId, name)
            authViewModel.stateModifyName.observe(viewLifecycleOwner){result->
                if(result){
                    Toast.makeText(requireContext(), "Modify Success", Toast.LENGTH_SHORT).show()
                    authViewModel.loadUser(userId)
                }
            }
            dialog.dismiss()
        }

        dialogBinding.imgButtonClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


