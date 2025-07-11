package com.example.doan13.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.data.repositories.FavoriteRepositories
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepositories
) : ViewModel() {
    private val _playlists = MutableLiveData<List<PlaylistModel>?>()
    val playlists: LiveData<List<PlaylistModel>?> get() = _playlists
    private val _playlistLiked = MutableLiveData<List<PlaylistModel>?>()
    val playlistLiked: LiveData<List<PlaylistModel>?> get() = _playlistLiked

    private val _createPlaylistResult = MutableLiveData<Result<String>?>()
    val createPlaylistResult: LiveData<Result<String>?> get() = _createPlaylistResult

    private val _deletePlaylistResult = MutableLiveData<Result<Unit>?>()
    val deletePlaylistResult: LiveData<Result<Unit>?> get() = _deletePlaylistResult
    private val _deletePlaylistLikedResult = MutableLiveData<Result<Unit>?>()
    val deletePlaylistLikedResult: LiveData<Result<Unit>?> get() = _deletePlaylistLikedResult

    private val _likeSongResult = MutableLiveData<Result<Unit>?>()
    val likeSongResult: LiveData<Result<Unit>?> get() = _likeSongResult

    private val _unlikeSongResult = MutableLiveData<Result<Unit>?>()
    val unlikeSongResult: LiveData<Result<Unit>?> get() = _unlikeSongResult

    private val _likePlaylistResult = MutableLiveData<Result<Unit>?>()
    val likePlaylistResult: LiveData<Result<Unit>?> get() = _likePlaylistResult

    private val _unlikePlaylistResult = MutableLiveData<Result<Unit>?>()
    val unlikePlaylistResult: LiveData<Result<Unit>?> get() = _unlikePlaylistResult

    private val _addSongToPlaylistResult = MutableLiveData<Result<Unit>?>()
    val addSongToPlaylistResult: LiveData<Result<Unit>?> get() = _addSongToPlaylistResult

    private  val _addPLaylistToPlaylistResult = MutableLiveData<Result<Unit>?>()
    val addPLaylistToPlaylistResult: LiveData<Result<Unit>?> get() = _addPLaylistToPlaylistResult
    val stateModifyName = MutableLiveData<Boolean>()
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading


    fun modifyName(userId: String, newName: String) {
        viewModelScope.launch {
            favoriteRepository.updateNamePlayList(userId, newName)
            stateModifyName.postValue(true)
        }
    }
    fun loadPlaylists(userId: String) {
        _loading.value = true
        viewModelScope.launch {
            _playlists.value = favoriteRepository.getUserPlaylists(userId)
            _loading.value =false
        }
    }
    fun loadLikedPlaylists(userId: String) {
        _loading.value = true
        viewModelScope.launch {
           _playlistLiked.value = favoriteRepository.getUserLikedPlaylists(userId)
            _loading.value =false
        }
    }

    fun createPlaylist(userId: String, name: String, firstSongId: String? = null) {
        viewModelScope.launch {
            _createPlaylistResult.value = favoriteRepository.createPlaylist(userId, name)
        }
    }

    fun deletePlaylist(playlistId: String, userId: String) {
        viewModelScope.launch {
            _deletePlaylistResult.value = favoriteRepository.deletePlaylist(playlistId, userId)
        }
    }
    fun deletePlaylistLiked(playlistId: String, userId: String) {
        viewModelScope.launch {
            _deletePlaylistLikedResult.value = favoriteRepository.deletePlaylistLiked(playlistId, userId)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        _loading.value = true
        viewModelScope.launch {
            _addSongToPlaylistResult.value = favoriteRepository.addSongToPlaylist(playlistId, songId)
            _loading.value = false
        }
    }
    fun addPlaylistToPlaylist(playlistId: String, userId : String) {
        _loading.value = true
        viewModelScope.launch {
            _addPLaylistToPlaylistResult.value = favoriteRepository.addUserToPlaylist(playlistId, userId)
            _loading.value = false
        }
    }




    fun updatePlayCount(songId: String) {
        viewModelScope.launch {
            favoriteRepository.updatePlayCount(songId)
        }
    }
    fun updatePlayCountOfPlaylist(playlistId: String) {
        viewModelScope.launch {
            favoriteRepository.updatePlayCountOfPlaylist(playlistId)
        }
    }




    fun resetCreatePlaylistResult() {
        _createPlaylistResult.value = null
    }

    fun resetaddPlaylistResult() {
        _addSongToPlaylistResult.value = null
    }
    fun resetaddPlaylisttopLAYLISTResult() {
        _addPLaylistToPlaylistResult.value = null
    }

    fun resetDeletePlaylistResult() {
        _deletePlaylistResult.value = null
    }
    fun resetDeletePlaylistLikedResult() {
        _deletePlaylistLikedResult.value = null
    }

    fun resetLoadPlaylist() {
        _playlists.value = null
    }
    fun resetLoadLikedPlaylist() {
        _playlistLiked.value = null
    }

}