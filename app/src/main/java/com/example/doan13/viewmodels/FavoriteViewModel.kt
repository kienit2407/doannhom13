package com.example.doan13.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.data.repositories.FavoriteRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepositories
) : ViewModel() {
    private val _playlists = MutableLiveData<List<PlaylistModel>?>()
    val playlists: LiveData<List<PlaylistModel>?> get() = _playlists

    private val _createPlaylistResult = MutableLiveData<Result<String>?>()
    val createPlaylistResult: LiveData<Result<String>?> get() = _createPlaylistResult

    private val _deletePlaylistResult = MutableLiveData<Result<Unit>?>()
    val deletePlaylistResult: LiveData<Result<Unit>?> get() = _deletePlaylistResult

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

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            _addSongToPlaylistResult.value = favoriteRepository.addSongToPlaylist(playlistId, songId)
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



    fun resetLikeSongResult() {
        _likeSongResult.value = null
    }

    fun resetUnlikeSongResult() {
        _unlikeSongResult.value = null
    }

    fun resetLikePlaylistResult() {
        _likePlaylistResult.value = null
    }

    fun resetUnlikePlaylistResult() {
        _unlikePlaylistResult.value = null
    }

    fun resetCreatePlaylistResult() {
        _createPlaylistResult.value = null
    }

    fun resetaddPlaylistResult() {
        _addSongToPlaylistResult.value = null
    }

    fun resetDeletePlaylistResult() {
        _deletePlaylistResult.value = null
    }

    fun resetLoadPlaylist() {
        _deletePlaylistResult.value = null
    }

}