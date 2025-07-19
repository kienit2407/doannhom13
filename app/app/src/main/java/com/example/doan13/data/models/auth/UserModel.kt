package com.example.doan13.data.models.auth

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp val createdAt: Date? = null,
    val provider: String = "", //trường lưu người đùng đăng nhập bằng phương thức nào email/gg
    val uploadedSongs: List<String> = emptyList(), //chứa những bài hát người dùng đã đăng
    val recentlyPlayed: List<String> = emptyList(),
    val playlists: List<String> = emptyList(),
    val playlistLiked: List<String> = emptyList(),
    ){
}