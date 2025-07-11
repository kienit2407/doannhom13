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
                onProgress((progress * 0.25).toInt()) // Cập nhật 50% cho thumbnail
            } ?: throw Exception("Không tải được thumbnail")

            // Tải MP3 lên Cloudinary (50% - 100%)
            val mp3Url = uploadToCloudinary(mp3File.absolutePath, "raw") { progress ->
                onProgress(25 + (progress * 0.75).toInt()) // Cập nhật 50% còn lại cho MP3
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
            val user = userDoc.toObject(UserModel::class.java) // truy cập vào field recent để lấy ra

            val songRecent = user?.recentlyPlayed ?: emptyList()

            if (songRecent.isNotEmpty()) {
                val songsQuery = db.collection("songs")
                    .whereIn("songId", songRecent)
                    .get()
                    .await()

                val songs = songsQuery.toObjects(SongModels::class.java)
                songs.sortedByDescending { songRecent.indexOf(it.songId)}

                Log.d("SongRepository", "Songs retrieved: ${songs.size}")

                songs
            } else {
                Log.d("SongRepository", "No uploaded songs found")
                emptyList()
            }

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
                .shuffled()
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
                .limit(10)
                .get()
                .await()
                .toObjects(PlaylistModel::class.java)

            playlists.filter { it.songIds.size >=2 }
                .distinctBy { it.playlistId }
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
                .distinctBy { it.playlistId }
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
                .distinctBy { it.playlistId }
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


    suspend fun getSongsByUserUploaded(userId: String): List<SongModels> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            val user = userDoc.toObject(UserModel::class.java)
            val songUploadedIds = user?.uploadedSongs ?: emptyList()
            if (songUploadedIds.isNotEmpty()) {
                 db.collection("songs")
                    .whereIn("songId", songUploadedIds)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects(SongModels::class.java)
            } else {
                Log.d("SongRepository", "No uploaded songs found")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SongRepository", "Error fetching songs: ${e.message}", e)
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





}

