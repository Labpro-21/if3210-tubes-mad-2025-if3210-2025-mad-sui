package com.vibecoder.purrytify.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val duration: Long? = null,
    val filePath: String? = null,
    val isLiked: Boolean = false
)
