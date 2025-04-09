package com.vibecoder.purrytify.playback

enum class RepeatMode {
    NONE, // No repeat
    ALL, // Repeat all songs
    ONE // Repeat one song
}

enum class ShuffleMode {
    ON,
    OFF
}

data class QueueItem(val songId: Long, val originalIndex: Int)
