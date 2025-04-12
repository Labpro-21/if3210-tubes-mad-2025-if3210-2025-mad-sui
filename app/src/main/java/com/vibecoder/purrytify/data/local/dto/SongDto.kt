package com.vibecoder.purrytify.data.local.dto

data class AddSongRequest(
    val title: String,
    val artist: String,
    val filePathUri: String,
    val coverArtUri: String?,
    val duration: Long,
    val isLiked: Boolean = false,
)

data class UpdateSongRequest(
    val id: Long,
    val title: String,
    val artist: String,
    val filePathUri: String,
    val coverArtUri: String?,
    val duration: Long,
    val isLiked : Boolean = false,
)