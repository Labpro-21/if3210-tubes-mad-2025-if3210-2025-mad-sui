package com.vibecoder.purrytify.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userEmail: String,
    val title: String,
    val artist: String,
    val filePathUri: String,
    val coverArtUri: String?,
    val duration: Long,
    val isLiked: Boolean = false,
    var isListened: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)