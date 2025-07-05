package com.example.doan13.data.repositories

import android.util.Log
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.data.models.auth.UserModel
import com.example.doan13.data.models.songs.SongModels
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.random.Random

class FavoriteRepositories @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

    // Tạo playlist mới
    suspend fun createPlaylist(
        userId: String,
        name: String,
    ): Result<String> {
        return try {
            // Tạo playlistId
            val playlistId = db.collection("playlists").document().id
            val playlistData = PlaylistModel(
                playlistId = playlistId,
                name = name,
                creatorId = userId,
                songIds = emptyList(), // Playlist rỗng
                thumbnailUrl = null, // Thumbnail rỗng khi tạo
                playCount = 0,
                createdAt = null, // ServerTimestamp sẽ được Firestore gán
            )

            // Lưu playlist vào Firestore
            db.collection("playlists").document(playlistId).set(playlistData).await()

            // Cập nhật playlistId vào user
            db.collection("users").document(userId)
                .update("playlists", FieldValue.arrayUnion(playlistId)).await()

            Result.success(playlistId) // TRẢ VỀ ID CỦA PLAYLIST
        } catch (e: Exception) {
            Log.e("SongRepository", "Lỗi tạo playlist: ${e.message}")
            Result.failure(e)
        }
    }


    // Thêm bài hát vào playlist và cập nhật thumbnail
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            // Lấy thông tin playlist hiện tại
            val playlistDoc = db.collection("playlists").document(playlistId).get().await()
            val playlist = playlistDoc.toObject(PlaylistModel::class.java)
                ?: throw Exception("Playlist không tồn tại")

            // Lấy danh sách songIds hiện tại
            val currentSongIds = playlist.songIds.toMutableList() ?: mutableListOf()
            if (!currentSongIds.contains(songId)) {
                currentSongIds.add(songId)

                // Cập nhật songIds
                db.collection("playlists").document(playlistId)
                    .update("songIds", currentSongIds).await()

                // Nếu là bài hát đầu tiên, cập nhật thumbnailUrl
                if (currentSongIds.size == 1) {
                    val firstSong = db.collection("songs").document(songId).get().await()
                        .toObject(SongModels::class.java)
                    val newThumbnailUrl = firstSong?.thumbnailUrl ?: ""
                    db.collection("playlists").document(playlistId)
                        .update("thumbnailUrl", newThumbnailUrl).await()
                }
            }


            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongRepository", "Lỗi thêm bài hát vào playlist: ${e.message}")
            Result.failure(e)
        }
    }

    // Xóa playlist
    suspend fun deletePlaylist(playlistId: String, userId: String): Result<Unit> {
        return try {
            db.collection("playlists").document(playlistId).delete().await()
            db.collection("users").document(userId)
                .update("playlists", FieldValue.arrayRemove(playlistId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongRepository", "Lỗi xóa playlist: ${e.message}")
            Result.failure(e)
        }
    }

    // Xóa bài hát khỏi playlist
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            db.collection("playlists").document(playlistId)
                .update("songIds", FieldValue.arrayRemove(songId)).await()

            // Cập nhật thumbnail
            val playlist = db.collection("playlists").document(playlistId).get().await()
                .toObject(PlaylistModel::class.java)
            if (playlist != null && playlist.songIds.isNotEmpty()) {
                val firstSong = db.collection("songs").document(playlist.songIds[0]).get().await()
                    .toObject(SongModels::class.java)
                if (firstSong != null) {
                    db.collection("playlists").document(playlistId)
                        .update("thumbnailUrl", firstSong.thumbnailUrl).await()
                }
            } else {
                db.collection("playlists").document(playlistId)
                    .update("thumbnailUrl", null).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongRepository", "Lỗi xóa bài hát khỏi playlist: ${e.message}")
            Result.failure(e)
        }
    }

    // Lấy danh sách playlist của user
    suspend fun getUserPlaylists(userId: String): List<PlaylistModel> {
        val playlistIds = db.collection("users").document(userId).get().await()
            .toObject(UserModel::class.java)?.playlists ?: emptyList()
        return if (playlistIds.isNotEmpty()) {
            db.collection("playlists")
                .whereIn("playlistId", playlistIds)
                .get()
                .await()
                .toObjects(PlaylistModel::class.java)
        } else {
            emptyList()
        }
    }

    // Lấy bài hát trong playlist
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

    // Thích bài hát
    suspend fun likeSong(userId: String, songId: String): Result<Unit> {
        return try {
            db.collection("songs").document(songId)
                .update("likedBy", FieldValue.arrayUnion(userId)).await()
            db.collection("users").document(userId)
                .update("likedSongs", FieldValue.arrayUnion(songId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FavoriteRepositories", "Lỗi thích bài hát: ${e.message}")
            Result.failure(e)
        }
    }

    // Bỏ thích bài hát
    suspend fun unlikeSong(userId: String, songId: String): Result<Unit> {
        return try {
            db.collection("songs").document(songId)
                .update("likedBy", FieldValue.arrayRemove(userId)).await()
            db.collection("users").document(userId)
                .update("likedSongs", FieldValue.arrayRemove(songId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FavoriteRepositories", "Lỗi bỏ thích bài hát: ${e.message}")
            Result.failure(e)
        }
    }

    // Thích playlist
    suspend fun likePlaylist(userId: String, playlistId: String): Result<Unit> {
        return try {
            db.collection("playlists").document(playlistId)
                .update("likedBy", FieldValue.arrayUnion(userId)).await()
            db.collection("users").document(userId)
                .update("likedPlaylists", FieldValue.arrayUnion(playlistId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FavoriteRepositories", "Lỗi thích playlist: ${e.message}")
            Result.failure(e)
        }
    }

    // Bỏ thích playlist
    suspend fun unlikePlaylist(userId: String, playlistId: String): Result<Unit> {
        return try {
            db.collection("playlists").document(playlistId)
                .update("likedBy", FieldValue.arrayRemove(userId)).await()
            db.collection("users").document(userId)
                .update("likedPlaylists", FieldValue.arrayRemove(playlistId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FavoriteRepositories", "Lỗi bỏ thích playlist: ${e.message}")
            Result.failure(e)
        }
    }


//    / Lấy thông tin playlist theo ID
    suspend fun getPlaylistById(playlistId: String): PlaylistModel? {
        return try {
            db.collection("playlists").document(playlistId).get().await()
                .toObject(PlaylistModel::class.java)
        } catch (e: Exception) {
            Log.e("PlaylistRepository", "Lỗi lấy playlist theo ID: ${e.message}", e)
            null
        }
    }
    // Cập nhật playCount khi bài hát được phát
    suspend fun updatePlayCount(songId: String) {
        try {
            val songDoc = db.collection("songs").document(songId).get().await()
            val currentPlayCount = songDoc.getLong("playCount")?.toInt() ?: 0
            db.collection("songs").document(songId)
                .update("playCount", currentPlayCount + 1)
                .addOnSuccessListener {
                    Log.d("FavoriteRepositories", "Updated playCount for songId: $songId to ${currentPlayCount + 1}")
                }
                .addOnFailureListener { e ->
                    Log.e("FavoriteRepositories", "Lỗi cập nhật playCount: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("FavoriteRepositories", "Lỗi lấy dữ liệu song: ${e.message}")
        }
    }
    // Cập nhật playCount khi bài hát được phát
    suspend fun updatePlayCountOfPlaylist(playlistId: String) {
        try {
            val playlistDoc = db.collection("playlists").document(playlistId).get().await()
            val currentPlayCount = playlistDoc.getLong("playCount")?.toInt() ?: 0

            db.collection("playlists").document(playlistId)
                .update("playCount", currentPlayCount + 1)
                .addOnSuccessListener {
                    Log.d("FavoriteRepositories", "Updated playCount for songId: $playlistId to ${currentPlayCount + 1}")
                }
                .addOnFailureListener { e ->
                    Log.e("FavoriteRepositories", "Lỗi cập nhật playCount: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("FavoriteRepositories", "Lỗi lấy dữ liệu song: ${e.message}")
        }
    }
    suspend fun updateNamePlayList(playlisId: String, newName: String): Result<Unit> {
        return try {
            FirebaseFirestore.getInstance().collection("playlists")
                .document(playlisId)
                .update("name", newName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Repository", "Lỗi cập nhật tên: ${e.message}")
            Result.failure(e)
        }
    }
}