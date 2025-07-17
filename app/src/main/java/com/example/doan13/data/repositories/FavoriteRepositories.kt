package com.example.doan13.data.repositories

import android.util.Log
import com.example.doan13.data.models.songs.PlaylistModel
import com.example.doan13.data.models.auth.UserModel
import com.example.doan13.data.models.songs.SongModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import okio.Source
import javax.inject.Inject
import kotlin.random.Random
interface FavoriteRepositories {
    suspend fun createPlaylist(userId: String, name: String): Result<String>
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Pair<Result<Unit>, String?>
    suspend fun addUserToPlaylist(playlistId: String, userId: String): Result<Unit>
    suspend fun deletePlaylist(playlistId: String, userId: String): Result<Unit>
    suspend fun updatePlayCount(songId: String)
    suspend fun deletePlaylistLiked(playlistId: String, userId: String): Result<Unit>
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit>
    suspend fun getUserPlaylists(userId: String): List<PlaylistModel>
    suspend fun getUserLikedPlaylists(userId: String): List<PlaylistModel>
    suspend fun getSongsInPlaylist(playlistId: String): List<SongModels>
    suspend fun updatePlayCountOfPlaylist(playlistId: String)
    suspend fun updateNamePlayList(playlistId: String, newName: String): Result<Unit>
}
class FavoriteRepositoriesImpl : FavoriteRepositories{
    private val db = FirebaseFirestore.getInstance()

    // Tạo playlist mới
    override suspend fun createPlaylist(
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
                songIds = emptyList(),
                thumbnailUrl = null,
                playCount = 0,
                createdAt = null,
                publicPlaylist = false,
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
   override suspend fun addSongToPlaylist(playlistId: String, songId: String): Pair<Result<Unit>, String?> {
        return try {
            // Lấy thông tin playlist hiện tại
            val playlistDoc = db.collection("playlists").document(playlistId).get().await()
            val playlist = playlistDoc.toObject(PlaylistModel::class.java)
                ?: throw Exception("Playlist không tồn tại")
            // Lấy danh sách songIds hiện tại
            val currentSongIds = playlist.songIds.toMutableList() ?: mutableListOf()
            // Kiểm tra nếu bài hát đã tồn tại
            if (currentSongIds.contains(songId)) {
                return Pair(Result.success(Unit), "Bài hát đã tồn tại trong playlist")
            }

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

            Pair(Result.success(Unit), "Thêm bài hát thành công")
        } catch (e: Exception) {
            Pair(Result.failure<Unit>(e), "Lỗi: ${e.message}")
        }
    }
    override suspend fun addUserToPlaylist(playlistId: String, userId: String): Result<Unit> {
        return try {
            val userDocRef = db.collection("users").document(userId)
            val userPlaylistDoc = userDocRef.get().await()
            val userPlaylist = userPlaylistDoc.toObject(UserModel::class.java)
                ?: throw Exception("User không tồn tại")

            // Lấy danh sách playlist hiện tại
            val currentPlaylists = userPlaylist.playlistLiked.toMutableList()

            // Kiểm tra xem playlist đã tồn tại chưa
            if (!currentPlaylists.contains(playlistId)) {
                currentPlaylists.add(playlistId)
                // CẬP NHẬT LẠI VÀO FIRESTORE
                userDocRef.update("playlistLiked", currentPlaylists).await()

                Log.d("SongRepository", "Thêm playlist $playlistId vào user $userId thành công")
                Result.success(Unit)
            } else {
                Log.d("SongRepository", "Playlist $playlistId đã tồn tại trong danh sách của user")
                Result.success(Unit) // Hoặc có thể return Result.failure nếu muốn báo lỗi
            }
        } catch (e: Exception) {
            Log.e("SongRepository", "Lỗi thêm playlist vào user: ${e.message}")
            Result.failure(e)
        }
    }

    // Xóa playlist
    override suspend fun deletePlaylist(playlistId: String, userId: String): Result<Unit> {
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
    override suspend fun deletePlaylistLiked(playlistId: String, userId: String): Result<Unit> {
        return try {

            db.collection("users").document(userId)
                .update("playlistLiked", FieldValue.arrayRemove(playlistId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongRepository", "Lỗi xóa playlist: ${e.message}")
            Result.failure(e)
        }
    }



    // Xóa bài hát khỏi playlist
    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit> {
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
    override suspend fun getUserPlaylists(userId: String): List<PlaylistModel> {
        val playlistIds = db.collection("users").document(userId).get().await()
            .toObject(UserModel::class.java)?.playlists ?: emptyList()
        return if (playlistIds.isNotEmpty()) {
            db.collection("playlists")
                .whereIn("playlistId", playlistIds)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
                .toObjects(PlaylistModel::class.java)
        } else {
            emptyList()
        }
    }
    override suspend fun getUserLikedPlaylists(userId: String): List<PlaylistModel> {
        val playlistIds = db.collection("users").document(userId).get().await()
            .toObject(UserModel::class.java)?.playlistLiked ?: emptyList()

        return if (playlistIds.isNotEmpty()) {
            db.collection("playlists")
                .whereIn("playlistId", playlistIds)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
                .toObjects(PlaylistModel::class.java)
        } else {
            emptyList()
        }
    }

    // Lấy bài hát trong playlist
    override suspend fun getSongsInPlaylist(playlistId: String): List<SongModels> {
        val playlist = db.collection("playlists").document(playlistId).get().await()
            .toObject(PlaylistModel::class.java)
        return if (playlist != null && playlist.songIds.isNotEmpty()) {
            db.collection("songs")
                .whereIn("songId", playlist.songIds)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
                .toObjects(SongModels::class.java)
        } else {
            emptyList()
        }
    }

    // Cập nhật playCount khi bài hát được phát
    override suspend fun updatePlayCount(songId: String) {
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
    override suspend fun updatePlayCountOfPlaylist(playlistId: String) {
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
    override suspend fun updateNamePlayList(playlisId: String, newName: String): Result<Unit> {
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