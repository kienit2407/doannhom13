package com.example.doan13.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.data.repositories.FavoriteRepositories
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val favoriteRepositories: FavoriteRepositories
) : ViewModel() {
    private val _playlist = MutableLiveData<PlaylistModel?>()
    val playlist: LiveData<PlaylistModel?> get() = _playlist

    private val _songs = MutableLiveData<List<SongModels>?>()
    val songs: LiveData<List<SongModels>?> get() = _songs

    private val _removeSongResult = MutableLiveData<Result<Unit>?>()
    val removeSongResult: LiveData<Result<Unit>?> get() = _removeSongResult

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    val db = FirebaseFirestore.getInstance()
//lấy ra những bài hát trong playlist
    fun loadSongInPlaylist(playlistId: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val playlistSnapshot = db.collection("playlists").document(playlistId).get().await()
                val playlist = playlistSnapshot.toObject(PlaylistModel::class.java)?.copy(playlistId = playlistId)
                _playlist.value = playlist
                _songs.value = favoriteRepositories.getSongsInPlaylist(playlistId)
            } catch (e: Exception) {
                _playlist.value = null
                _songs.value = emptyList()
            }
            _loading.value = false
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {

        viewModelScope.launch {
            _removeSongResult.value = favoriteRepositories.removeSongFromPlaylist(playlistId, songId)

        }
    }


    fun resetRemoveSongFromPlaylist (){
        _removeSongResult.value = null
    }

}