package com.example.doan13.viewmodels

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.data.models.auth.UserModel
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.data.repositories.SongRepositories
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import javax.inject.Inject

@HiltViewModel
class SongViewModel @Inject constructor(
    private val songRepository: SongRepositories
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _recentSongs = MutableLiveData<List<SongModels>?>()
    val recentSongs: LiveData<List<SongModels>?> get() = _recentSongs

    private val _artists = MutableLiveData<List<UserModel>?>()
    val artists: LiveData<List<UserModel>?> get() = _artists

    private val _newTracks = MutableLiveData<List<SongModels>?>()
    val newTracks: LiveData<List<SongModels>?> get() = _newTracks

    private val _popularPlaylists = MutableLiveData<List<PlaylistModel>?>()
    val popularPlaylists: LiveData<List<PlaylistModel>?> get() = _popularPlaylists

    private val _recommendedTracks = MutableLiveData<List<SongModels>?>()
    val recommendedTracks: LiveData<List<SongModels>?> get() = _recommendedTracks

    private val _recommendedPlaylists = MutableLiveData<List<PlaylistModel>?>()
    val recommendedPlaylists: LiveData<List<PlaylistModel>?> get() = _recommendedPlaylists

    private val _searchTracks = MutableLiveData<List<SongModels>>() // Kết quả track
    val searchTracks: LiveData<List<SongModels>> get() = _searchTracks

    private val _searchPlaylists = MutableLiveData<List<PlaylistModel>>() // Kết quả playlist
    val searchPlaylists: LiveData<List<PlaylistModel>> get() = _searchPlaylists

    private val userCache = mutableMapOf<String, String>()
    // LiveData cho danh sách bài hát đã đăng
    private val _uploaderSongs = MutableLiveData<List<SongModels>?>()
    val uploaderSongs: LiveData<List<SongModels>?> get() = _uploaderSongs

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _removeSongs = MutableLiveData<Result<Unit>?>()
    val removeSongs: LiveData<Result<Unit>?> get() = _removeSongs

    private val _getOtherSongs = MutableLiveData<List<SongModels>>()
    val getOtherSongs: LiveData<List<SongModels>> get() = _getOtherSongs

    private var _isUploaded = MutableLiveData<Boolean?>()
    val isUploaded: LiveData<Boolean?> = _isUploaded
    private var _isUploadedThumnailTrack = MutableLiveData<Boolean?>()
    val isUploadedThumnailTrack: LiveData<Boolean?> = _isUploadedThumnailTrack
    private var _isUploadedThumnailPlaylist = MutableLiveData<Boolean?>()
    val isUploadedThumnailPlaylist: LiveData<Boolean?> = _isUploadedThumnailPlaylist

    private val _isPublic = MutableLiveData<Boolean>(false)
    val isPublic: LiveData<Boolean> = _isPublic

    private val _isPublicPlaylist = MutableLiveData<Boolean>(false)
    val isPublicPlaylist: LiveData<Boolean> = _isPublicPlaylist

    fun loadPublicStatusForTrack(songId: String) {
        viewModelScope.launch {
            try {
                val isPublic = songRepository.getPublicStatusForTrack(songId) // Giả sử có phương thức này
                _isPublic.value = isPublic
            } catch (e: Exception) {
                Log.e("FavoriteViewModel", "Lỗi tải trạng thái public: ${e.message}")
            }
        }
    }

    fun loadPublicStatusForPlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                val isPublic = songRepository.getPublicStatusForPlaylist(playlistId) // Giả sử có phương thức này
                _isPublicPlaylist.value = isPublic
            } catch (e: Exception) {
                Log.e("FavoriteViewModel", "Lỗi tải trạng thái public: ${e.message}")
            }
        }
    }
    fun updatePublicStatusForPlaylist(playlistId: String, isPublic: Boolean) {
        viewModelScope.launch {
            try {
                songRepository.updatePublicStatusForPlaylist(playlistId, isPublic)
            } catch (e: Exception) {
                Log.e("FavoriteViewModel", "Lỗi: ${e.message}")

            }
        }
    }
    fun updatePublicStatus(songId: String, isPublic: Boolean) {
        viewModelScope.launch {
            try {
                songRepository.updatePublicStatusForTrack(songId, isPublic)
                _isPublic.value = isPublic
//                _message.value = "Cập nhật trạng thái công khai thành công"
            } catch (e: Exception) {
                Log.e("FavoriteViewModel", "Lỗi: ${e.message}")
//                _message.value = "Lỗi: ${e.message}"
            }
//            _message.value = null
        }
    }
    // Lấy thông tin người dùng và danh sách bài hát đã đăng
    fun getUserAndUploadedSongs(userId: String) {
        _loading.value = true
        viewModelScope.launch {
            _loading.value = true
            try {
                _uploaderSongs.value = songRepository.getSongsByUserUploaded(userId)
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error fetching data: ${e.message}")
                _uploaderSongs.postValue(emptyList())
            }
            _loading.value =false
        }
    }


    fun getUserName(userId: String, callback: (String) -> Unit) {
        // Kiểm tra xem tên người dùng đã có trong cache chưa
        val cachedUserName = userCache[userId]
        if (cachedUserName != null) {
            // Nếu có trong cache, trả về luôn
            callback(cachedUserName)
        } else {
            // Nếu chưa có, tải từ Firestore và lưu vào cache
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val userName = document?.getString("name") ?: "Unknown User"
                    // Lưu vào cache
                    userCache[userId] = userName
                    callback(userName)
                }
                .addOnFailureListener {
                    callback("Unknown User")
                }
        }
    }

    //dùng để lấy avt trong public playlist detail
    fun getAvatarByUserId(userId: String, callback: (String) -> Unit) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val avatarUrl = document?.getString("imageUrl") ?: "https://via.placeholder.com/150" // URL mặc định
                Log.d("SongRepository", "Lấy avatar thành công cho userId: $userId - URL: $avatarUrl")
                callback(avatarUrl)
            }
            .addOnFailureListener { e ->
                Log.e("SongRepository", "Lỗi khi lấy avatar cho userId: $userId - ${e.message}")
                callback("https://via.placeholder.com/150") // URL mặc định khi lỗi
            }
    }
    fun loadData(userId :String) {
        _loading.value = true
        viewModelScope.launch {
            if(
                  _recentSongs.value.isNullOrEmpty()
                ||_artists.value.isNullOrEmpty()
                ||_newTracks.value.isNullOrEmpty()
                ||_popularPlaylists.value.isNullOrEmpty()
                ||_recommendedTracks.value.isNullOrEmpty()
                ||_recommendedPlaylists.value.isNullOrEmpty()
                ){
                _recentSongs.value = songRepository.getRecentSongs(userId)
                _artists.value = songRepository.getArtists(userId)
                _newTracks.value = songRepository.getNewTracks(userId)
                _popularPlaylists.value = songRepository.getPopularPlaylists(userId)
                _recommendedTracks.value = songRepository.getRecommendedTracks(userId)
                _recommendedPlaylists.value = songRepository.getRecommendedPlaylists(userId)
            }
            _loading.value = false
        }
    }
    fun loadDataWipe(userId :String) {
        _loading.value = true
        viewModelScope.launch {
            _recentSongs.value = songRepository.getRecentSongs(userId)
            _artists.value = songRepository.getArtists(userId)
            _newTracks.value = songRepository.getNewTracks(userId)
            _popularPlaylists.value = songRepository.getPopularPlaylists(userId)
            _recommendedTracks.value = songRepository.getRecommendedTracks(userId)
            _recommendedPlaylists.value = songRepository.getRecommendedPlaylists(userId)
            //load
            _loading.value = false
        }
    }

    fun removeSong(songId : String, userId: String){
       viewModelScope.launch {
           _loading.value = true
           try {
               _removeSongs.value = songRepository.removeSong(userId, songId)
           }catch (e:Exception){
               Log.e("","")
           }
       }
    }



    // Chuẩn hóa chuỗi: loại bỏ dấu thanh và chuyển về lowercase, cái này giúp truỳ vấn theo 3 kiểu: có dấu, không dấu và hỗn hợp,

    private fun normalizeString(input: String): String {
        // Loại bỏ dấu thanh (diacritics) bằng Normalizer
        //phần tách và loại bỏ dấu thanh và để đảm bảo không phân biẹt hoa thường bằng cách chuyển về lowercase
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        return regex.replace(normalized, "").lowercase()
    }

    fun search(query: String) {
        _loading.value = true
        viewModelScope.launch {

            if (query.trim().isEmpty()) {
                _searchTracks.postValue(emptyList())
                _searchPlaylists.postValue(emptyList())
                return@launch
            }

            try {
                // Lấy tất cả dữ liệu
                val allSongs = songRepository.getAllSongs()
                val allPlaylists = songRepository.getAllPlaylists()
                val allUsers = songRepository.getAllUsers()

                // Tạo map: userId -> userName (để tìm theo tên người đăng)
                val userMap = allUsers.associate { it.uid to it.name }

                val songMap = allSongs.associateBy { it.songId }
                // Chuẩn hóa truy vấn tìm kiếm
                val searchQuery = normalizeString(query.trim())


                // Tìm kiếm bài hát
                val filteredSongs = allSongs.filter { song ->
                    val titleNormalized = normalizeString(song.title)
                    val artistNormalized = normalizeString(song.artist)
                    val userNameNormalized = song.userId.let { userMap[it] }?.let { normalizeString(it) } ?: ""

                    titleNormalized.contains(searchQuery) ||
                            artistNormalized.contains(searchQuery) ||
                            userNameNormalized.contains(searchQuery)
                }

                // Tìm kiếm playlist
                val filteredPlaylists = allPlaylists.filter { playlist ->
                    val nameNormalized = normalizeString(playlist.name)
                    val creatorNameNormalized = playlist.creatorId.let { userMap[it] }?.let { normalizeString(it) } ?: ""
                    // 1. Kiểm tra tên playlist hoặc người tạo
                    val matchNameOrCreator = nameNormalized.contains(searchQuery) || creatorNameNormalized.contains(searchQuery)

                    // 2. Kiểm tra tên bài hát trong playlist
                    val matchSongTitles = playlist.songIds?.any { songId ->
                        songMap[songId]?.let { song ->
                            normalizeString(song.title).contains(searchQuery) ||
                                    normalizeString(song.artist).contains(searchQuery)
                        } ?: false
                    } ?: false

                    // Kết hợp cả 2 điều kiện
                    matchNameOrCreator || matchSongTitles
                }

                // Cập nhật kết quả
                _searchTracks.postValue(filteredSongs)
                _searchPlaylists.postValue(filteredPlaylists)

                Log.d("SongViewModel", "Tìm thấy: ${filteredSongs.size} bài hát, ${filteredPlaylists.size} playlist")

            } catch (e: Exception) {
                Log.e("SongViewModel", "Lỗi tìm kiếm: ${e.message}")
                _searchTracks.postValue(emptyList())
                _searchPlaylists.postValue(emptyList())
            }
            _loading.value = false
        }
    }

    //reset
  fun resetuploaderSongs(){
      _uploaderSongs.value = null
  }
    fun resetremoveSongs(){
      _removeSongs.value = null
  }

}