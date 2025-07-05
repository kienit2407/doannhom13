package com.example.doan13.data.models.auth

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp val createdAt: Date? = null,
    val provider: String = "",
    val uploadedSongs: List<String> = emptyList(),
    val recentlyPlayed: List<String> = emptyList(),
    val playlists: List<String> = emptyList(), // Thêm trường playlists
    ){
}