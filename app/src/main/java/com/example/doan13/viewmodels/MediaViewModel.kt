package com.example.doan13.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.data.repositories.FavoriteRepositories
import com.example.doan13.data.repositories.SongRepositories
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val songRepositories: SongRepositories,
    @ApplicationContext private val context: Context
) : ViewModel() {


    private var _player: ExoPlayer? = null //tạo đối tượng của ExoPlayler. khỏi toạ ban đầu là null
    val player: ExoPlayer
        get() {
            if (_player == null) {
                _player = ExoPlayer.Builder(getApplication(context)).build().apply {
                    addListener(playerListener)
                }
            }
            return _player!!
        }

    // LiveData cho UI observe
    private val _currentSong = MutableLiveData<SongModels?>()
    val currentSong: LiveData<SongModels?> = _currentSong

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _playlist = MutableLiveData<List<SongModels>>(emptyList())
    val playlist: LiveData<List<SongModels>> = _playlist

    private val _isShuffled = MutableLiveData<Boolean>(false)
    val isShuffled: LiveData<Boolean> = _isShuffled

    private val _repeatMode = MutableLiveData<RepeatMode>(RepeatMode.OFF)
    val repeatMode: LiveData<RepeatMode> = _repeatMode

    private val _currentIndex = MutableLiveData<Int>(0)
    val currentIndex: LiveData<Int> = _currentIndex

    // Player listener
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    Log.e("playerListener", "Nhạc đã sẵn sàng")
                    // Cập nhật UI khi sẵn sàng
                    updateCurrentSongFromPlayer() // cập nhật vị trí hiện tại của bài hát đang phát
                }
                Player.STATE_ENDED -> {
                    handlePlaybackEnded() // xử lý sau khi một bài hát sau kh phát xong
                }
                Player.STATE_BUFFERING -> {
                    Log.e("playerListener", "Nhạc đang phát")
                }

            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Cập nhật index để đưa ra ui khi chuyển bài
            updateCurrentSongFromPlayer()
            Log.d("MediaViewModel", "Media item transition: ${mediaItem?.mediaId}, reason: $reason")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            //theo dõi dự thay đổi của play/pause của bài hát
            _isPlaying.postValue(isPlaying)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) { //nếu lỗi thì làm gì
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                retryPlayback()
            }
        }
    }

    // Hàm cập nhật currentSong từ player state
    private fun updateCurrentSongFromPlayer() {
        val currentIndex = player.currentMediaItemIndex
        val playlist = _playlist.value ?: return

        if (currentIndex >= 0 && currentIndex < playlist.size) {
            _currentIndex.value = currentIndex
            _currentSong.value = playlist[currentIndex]
        }
    }

    // Thiết lập playlist và phát
    fun setPlaylistAndPlay(songs: List<SongModels>, startIndex: Int = 0, shuffle: Boolean = false) {
        val playList = if (shuffle) songs.shuffled() else songs
        _playlist.value = playList
        _isShuffled.value = shuffle

        if (playList.isNotEmpty()) {
            val mediaItems = playList.map { song ->
                MediaItem.Builder()
                    .setUri(song.mp3Url ?: "")
                    .setMediaId(song.songId)
                    .build()
            }

            player.setMediaItems(mediaItems)
            player.shuffleModeEnabled = shuffle
            player.seekToDefaultPosition(startIndex)
            player.prepare()

            _currentSong.value = playList[startIndex]
            _currentIndex.value = startIndex

            play()
        }
    }

    // Phát một bài hát dựa trên songId
    fun setSongAndPlay(songId: String) {
        viewModelScope.launch {
            try {
                val song = songRepositories.getSongById(songId)
                if (song != null && song.mp3Url.isNotEmpty()) {
                    val mediaItem = MediaItem.Builder()
                        .setUri(song.mp3Url)
                        .setMediaId(song.songId)
                        .build()

                    player.setMediaItem(mediaItem)
                    player.prepare()

                    _currentSong.value = song
                    _currentIndex.value = 0 // Reset index vì chỉ có 1 bài
                    _playlist.value = listOf(song) // Cập nhật playlist chỉ chứa bài hát này
                    play()
                } else {
                    Log.e("MediaViewModel", "Bài hát với songId $songId không tìm thấy hoặc mp3Url trống")
                }
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Lỗi khi lấy và phát bài hát: ${e.message}")
            }
        }
    }

    fun setPlaying(){
        pause()
    }
    // Phát/dừng
    fun togglePlayPause() {
        if (_isPlaying.value == true) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (player.playbackState == Player.STATE_IDLE && _currentSong.value != null) {
            setSongAndPlay(_currentSong.value!!.songId) // Tái khởi động nếu player ở trạng thái idle
        } else {
            player.play()
            _isPlaying.value = true
        }
    }

    fun pause() {
        player.pause()
        _isPlaying.value = false
    }

    fun playNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        } else {
            // Nếu không có bài tiếp theo, quay về bài đầu (cho chế độ lặp)
            player.seekToDefaultPosition(0)
        }
    }

    fun playPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        } else {
            // Nếu không có bài trước, quay về bài cuối
            val lastIndex = (_playlist.value?.size ?: 1) - 1
            player.seekToDefaultPosition(lastIndex)
        }
    }

    // Toggle shuffle

    fun toggleShuffle() {
        val newShuffleState = !(_isShuffled.value ?: false)
        _isShuffled.value = newShuffleState

        if (newShuffleState) {
            // Bật shuffle - chỉ bật shuffle mode của ExoPlayer
            player.shuffleModeEnabled = true
        } else {
            // Tắt shuffle
            player.shuffleModeEnabled = false
        }
    }

    // Nút chế độ lặp lại
    fun toggleRepeatMode() {
        val newMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.REPEAT_ALL
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.OFF
            null -> RepeatMode.OFF
        }
        _repeatMode.value = newMode
        Log.e("Repeatmode", "${_repeatMode.value}")

        player.repeatMode = when (newMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
        }
    }

    // Xử lý khi phát xong một bài
    // Xử lý khi phát xong một bài
    private fun handlePlaybackEnded() {
        when (_repeatMode.value) {
            RepeatMode.REPEAT_ONE -> {
                player.seekTo(0)
                player.play()
            }
            RepeatMode.REPEAT_ALL -> {}
            RepeatMode.OFF -> {
                player.seekTo(0)
                player.pause()
            }
            null -> return
        }
    }


    private fun retryPlayback() {
        viewModelScope.launch {
            delay(2000) // Đợi 1 giây trước khi thử lại
            if (_currentSong.value != null && !_isPlaying.value!!) {
                setSongAndPlay(_currentSong.value!!.songId)
            }
        }
    }


    fun getCurrentPosition(): Long = player.currentPosition //lất thời gian của vị trí hiện tại
    fun getDuration(): Long = player.duration // lấy tổng thời gian của bài hát

    // Seek đến vị trí cụ thể
    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    override fun onCleared() {
        super.onCleared()
        _player?.release()
        _player = null
    }

    enum class RepeatMode {
        OFF, REPEAT_ALL, REPEAT_ONE
    }
}