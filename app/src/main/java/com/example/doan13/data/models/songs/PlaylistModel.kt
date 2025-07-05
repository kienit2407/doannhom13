package com.example.doan13.data.models.songs

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class PlaylistModel(
    val playlistId: String = "",
    val name: String = "",
    val creatorId: String = "",
    val songIds: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val playCount: Int = 0,
    @ServerTimestamp val createdAt: Date? = null,
) {
}