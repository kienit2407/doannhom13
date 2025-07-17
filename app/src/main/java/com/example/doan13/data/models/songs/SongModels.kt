package com.example.doan13.data.models.songs

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SongModels (
    val songId: String = "",
    val title : String="",
    val thumbnailUrl : String = "",
    val artist : String = "",
    val mp3Url: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Date? = null,
    val playCount: Int = 0,
    val publicTrack: Boolean = false,
){
}