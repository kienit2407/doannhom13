package com.example.doan13.data.repositories

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.utils.ObjectUtils
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.data.models.auth.UserModel
import com.example.doan13.data.models.songs.SongModels
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.random.Random

class SongRepositories {
    private val db = FirebaseFirestore.getInstance()
   private val userId = FirebaseAuth.getInstance().currentUser?.uid
    suspend fun uploadSong(
        thumbnail: File,
        mp3File: File,
        title: String,
        artist: String,
        uploaderId: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        return try {
            // Kiểm tra file trước khi tải lên
            if (!thumbnail.exists() || !thumbnail.canRead()) {
                throw Exception("Thumbnail file không hợp lệ: ${thumbnail.absolutePath}")
            }
            if (!mp3File.exists() || !mp3File.canRead()) {
                throw Exception("MP3 file không hợp lệ: ${mp3File.absolutePath}")
            }

//            / Tải thumbnail lên Cloudinary (0% - 50%)
            val thumbnailUrl = uploadToCloudinary(thumbnail.absolutePath, "image") { progress ->
                onProgress((progress * 0.5).toInt()) // Cập nhật 50% cho thumbnail
            } ?: throw Exception("Không tải được thumbnail")

            // Tải MP3 lên Cloudinary (50% - 100%)
            val mp3Url = uploadToCloudinary(mp3File.absolutePath, "raw") { progress ->
                onProgress(50 + (progress * 0.5).toInt()) // Cập nhật 50% còn lại cho MP3
            } ?: throw Exception("Không tải được MP3")
            // Lưu thông tin bài hát vào Firestore

            // Tạo songId trước
            val songId = db.collection("songs").document().id
            val songData = SongModels(
                songId = songId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                mp3Url = mp3Url,
                userId = uploaderId,
                createdAt = null, // ServerTimestamp sẽ được Firestore gán
                playCount = 0,
            )

            // Lưu bài hát vào Firestore
            db.collection("songs").document(songId).set(songData).await()

            // Cập nhật uploadedSongs của user
            db.collection("users").document(uploaderId)
                .update("uploadedSongs", FieldValue.arrayUnion(songId)).await()

            Result.success(thumbnailUrl)
        } catch (e: Exception) {
            Log.e("SongRepository", "Lỗi tải lên bài hát: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun uploadToCloudinary(
        filePath: String,
        resourceType: String,
        progressCallback: (Int) -> Unit
    ): String? {
        return suspendCancellableCoroutine { continuation ->
            try {
                MediaManager.get().upload(filePath)
                    .option("resource_type", resourceType)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d("Cloudinary", "Bắt đầu tải lên $resourceType: $requestId")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes * 100 / totalBytes).toInt()
                            Log.d("Cloudinary", "Tiến trình $resourceType: $progress%")
                            progressCallback(progress) // Gửi tiến trình qua callback
                        }

                        override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>?) {
                            val url = resultData?.get("url") as? String
                            Log.d("Cloudinary", "Tải lên $resourceType thành công: $url")
                            continuation.resume(url)
                        }

                        override fun onError(requestId: String, error: ErrorInfo?) {
                            Log.e("Cloudinary", "Lỗi tải lên $resourceType: ${error?.description}")
                            continuation.resume(null)
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo?) {
                            Log.w("Cloudinary", "Tải lên $resourceType được lên lịch lại: ${error?.description}")
                            continuation.resume(null)
                        }
                    })
                    .dispatch()
            } catch (e: Exception) {
                Log.e("Cloudinary", "Lỗi khi gọi upload: ${e.message}")
                continuation.resume(null)
            }
        }
    }
    suspend fun updateAvatar(
        avatarFile: File,
        userId: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        return try {
            // Kiểm tra file
            if (!avatarFile.exists() || !avatarFile.canRead()) {
                throw Exception("Avatar file không hợp lệ: ${avatarFile.absolutePath}")
            }

            // Upload avatar mới lên Cloudinary
            val newAvatarUrl = uploadToCloudinary(avatarFile.absolutePath, "image") { progress ->
                onProgress((progress * 0.8).toInt()) // 80% cho upload
            } ?: throw Exception("Không tải được avatar")

            // Cập nhật URL mới trong Firebase
            db.collection("users").document(userId)
                .update("imageUrl", newAvatarUrl).await()

            onProgress(90) // 90% sau khi cập nhật Firebase

            onProgress(100) // Hoàn thành

            Result.success(newAvatarUrl)
        } catch (e: Exception) {
            Log.e("Repository", "Lỗi cập nhật avatar: ${e.message}")
            Result.failure(e)
        }
    }

// hàm lấy ra những bài hát gần đây đã
    suspend fun getRecentSongs(userId: String,): List<SongModels> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await() //lấy ra doc của user
            val songIds = userDoc.get("recentlyPlayed") as? List<String> ?: emptyList() // truy cập vào field recent để lấy ra
            //dánh sách các id bài hát gần đây

            val recentSongIds = songIds.take(20) //lấy ra 20 bài trước
            val chunks = recentSongIds.chunked(10)
            val songs = mutableListOf<SongModels>()

            chunks.forEach { chunk ->
                val chunkResults = db.collection("songs")
                    .whereIn("songId", chunk)
                    .get()
                    .await()
                    .toObjects(SongModels::class.java)
                songs.addAll(chunkResults)
            }
            // Sắp xếp lại theo thứ tự trong recentSongIds
            songs.sortedByDescending { recentSongIds.indexOf(it.songId)}
        } catch (e: Exception) {
            Log.e("SongRepositories", "Lỗi khi lấy bài hát gần đây: ${e.message}")
            emptyList()
        }
    }

    suspend fun getArtists(currentUserId: String): List<UserModel> { //lấy ra tất cả user
        return try {
            db.collection("users")
                .whereNotEqualTo("uid", currentUserId)
                .get()
                .await()
                .toObjects(UserModel::class.java)
        } catch (e: Exception) {
            Log.e("SongRepositories", "Error getting artists: ${e.message}")
            emptyList()
        }
    }
//lấy ra những bài hát mới nhất
    suspend fun getNewTracks(currentUserId: String): List<SongModels> {
        return try {
            db.collection("songs")
                .whereNotEqualTo("userId", currentUserId) // Lọc bài hát của người dùng khác
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
                .toObjects(SongModels::class.java)
        } catch (e: Exception) {
            Log.e("SongRepositories", "Error getting new tracks: ${e.message}")
            emptyList()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    suspend fun getPopularPlaylists(currentUserId: String): List<PlaylistModel> {
        return try {
         val playlists = db.collection("playlists")
                .whereNotEqualTo("creatorId", currentUserId) // Lọc playlist của người dùng khác
                .orderBy("playCount", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(PlaylistModel::class.java)

            playlists.filter { it.songIds.size >=2 }
        } catch (e: Exception) {
            Log.e("SongRepositories", "Error getting popular playlists: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRecommendedTracks(currentUserId: String): List<SongModels> {
        return try {
            db.collection("songs")
                .whereNotEqualTo("userId", currentUserId) // Lọc bài hát của người dùng khác
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
                .toObjects(SongModels::class.java)
                .shuffled() // Xáo trộn
        } catch (e: Exception) {
            Log.e("SongRepositories", "Error getting recommended tracks: ${e.message}")
            emptyList()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    suspend fun getRecommendedPlaylists(currentUserId: String): List<PlaylistModel> {
        return try {
      val playlists =  db.collection("playlists")
                .whereNotEqualTo("creatorId", currentUserId) // Lọc playlist của người dùng khác
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(PlaylistModel::class.java)
                .shuffled() // Xáo trộn để thay thế "random"
            playlists.filter { it.songIds.size >=2 }
        } catch (e: Exception) {
            Log.e("SongRepositories", "Error getting recommended playlists: ${e.message}")
            emptyList()
        }
    }



    //hàm xử lý phàn tìm kiếm
    suspend fun getAllSongs(): List<SongModels> {
        return try {
            db.collection("songs")
                .whereNotEqualTo("userId", userId) // Lọc bài hát của người dùng khác
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(SongModels::class.java) }
        } catch (e: Exception) {
            println("Lỗi lấy tất cả bài hát: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllPlaylists(): List<PlaylistModel> {
        return try {
         val playlists =  db.collection("playlists")
                .whereNotEqualTo("creatorId", userId) // Lọc bài hát của người dùng khác
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(PlaylistModel::class.java) }
            playlists.filter { it.songIds.size >=2 }
        } catch (e: Exception) {
            println("Lỗi lấy tất cả playlist: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllUsers(): List<UserModel> {
        return try {
            db.collection("users")
                .whereNotEqualTo("uid", userId) // Lọc bài hát của người dùng khác
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(UserModel::class.java) }
        } catch (e: Exception) {
            println("Lỗi lấy tất cả user: ${e.message}")
            emptyList()
        }
    }


    suspend fun getSongById(songId: String): SongModels? {
        return try {
            FirebaseFirestore.getInstance().collection("songs")
                .document(songId)
                .get()
                .await()
                .toObject(SongModels::class.java)
        } catch (e: Exception) {
            Log.e("SongRepository", "Lỗi lấy bài hát: ${e.message}")
            null
        }
    }
    suspend fun getSongsInPlaylist(playlistId: String): List<SongModels> {
        val playlist = db.collection("playlists").document(playlistId).get().await()
            .toObject(PlaylistModel::class.java)
        return if (playlist != null && playlist.songIds.isNotEmpty()) {
            db.collection("songs")
                .whereIn("songId", playlist.songIds)
                .get()
                .await()
                .toObjects(SongModels::class.java)
        } else {
            emptyList()
        }
    }
    suspend fun getSongsByUser(userId: String): List<SongModels> {
        return try {
            db.collection("songs")
                .whereEqualTo("uid", userId) // Lọc theo userId
                .get()
                .await()
                .toObjects(SongModels::class.java)
        } catch (e: Exception) {
            Log.e("SongViewModel", "Error fetching songs by user: ${e.message}")
            emptyList()
        }
    }
    // Lấy thông tin người dùng dựa trên userId
    suspend fun getUserById(userId: String): UserModel? {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("uid", userId)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(UserModel::class.java)
        } catch (e: Exception) {
            Log.e("SongRepository", "Error fetching user: ${e.message}")
            null
        }
    }

    suspend fun getSongsByIds(songIds : List<String>): List<SongModels> {
        return try {
            if (songIds.isNotEmpty()) {
                db.collection("songs")
                    .whereIn("songId", songIds)
                    .get()
                    .await()
                    .toObjects(SongModels::class.java)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SongRepository", "Error fetching songs: ${e.message}")
            emptyList()
        }
    }
    // Xóa bài hát khỏi playlist
    suspend fun removeSong(userId: String, songId: String): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update("uploadedSongs", FieldValue.arrayRemove(songId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongRepository", "Lỗi xóa bài hát khỏi playlist: ${e.message}")
            Result.failure(e)
        }
    }
    suspend fun updateAvt(newImgUrl: Uri): Result<Unit> {
        return try {
            if (userId != null) {
                FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .update("imageUrl", newImgUrl)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Repository", "Lỗi cập nhật tên: ${e.message}")
            Result.failure(e)
        }
    }
    suspend fun updateAvtTracks(newImgUrl: Uri, songId: String): Result<Unit> {
        return try {

                FirebaseFirestore.getInstance().collection("songs")
                    .document(songId)
                    .update("thumbnailUrl", newImgUrl)
                    .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Repository", "Lỗi cập nhật tên: ${e.message}")
            Result.failure(e)
        }
    }
    suspend fun updateAvtPlaylist(newImgUrl: Uri, playlistId: String): Result<Unit> {
        return try {

                FirebaseFirestore.getInstance().collection("playlists")
                    .document(playlistId)
                    .update("thumbnailUrl", newImgUrl)
                    .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Repository", "Lỗi cập nhật tên: ${e.message}")
            Result.failure(e)
        }
    }



}

