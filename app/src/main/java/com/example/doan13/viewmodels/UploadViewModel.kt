package com.example.doan13.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doan13.data.repositories.SongRepositories
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val repository: SongRepositories
) : ViewModel() {
    private val _uploadProgress = MutableLiveData<Int?>(0) // Thêm LiveData cho tiến trình
    val uploadProgress: LiveData<Int?> = _uploadProgress

    private val _uploadState = MutableLiveData<Result<String>?>()
    val uploadState: LiveData<Result<String>?> get() = _uploadState
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _updateAvtState = MutableLiveData<Result<String>?>()
    val updateAvtState: LiveData<Result<String>?> get() = _updateAvtState

    private val _updateAvtProgress = MutableLiveData<Int>(0) // Thêm LiveData cho tiến trình
    val updateAvtProgress: LiveData<Int> = _updateAvtProgress


    var uploadSongSession: Job? = null




    fun uploadSong(thumbnail: File, mp3File: File, title: String, artist: String, uploaderId: String) {
        uploadSongSession =  viewModelScope.launch {
            _uploadProgress.value = 0 // Reset tiến trình
            repository.uploadSong(thumbnail, mp3File, title, artist, uploaderId) { progress ->
                _uploadProgress.postValue(progress) // Cập nhật tiến trình lên UI
            }.let { result ->
                _uploadState.value = result
            }
        }
    }
    // Hàm để hủy quá trình tải lên
    fun cancelUpload() {
        uploadSongSession?.cancel() // Hủy coroutine
    }
    // Hàm để upload avatar
    fun updateUserAvatar(avatarFile: File, userId: String) {
        _loading.value = true
        viewModelScope.launch {
            repository.updateAvatar(avatarFile, userId) { progress ->
                _updateAvtProgress.postValue(progress)
            }.let { result ->
                _updateAvtState.value = result
            }
            _loading.value = false
        }
    }
    fun removeUploadState (){
        viewModelScope.launch {
            _uploadState.value = null
            _uploadProgress.value = null
        }
    }

}