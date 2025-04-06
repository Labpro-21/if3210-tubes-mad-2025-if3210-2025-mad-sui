package com.vibecoder.purrytify.presentation.features.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoder.purrytify.presentation.components.MinimizedMusicPlayer
import com.vibecoder.purrytify.presentation.features.player.PlayerViewModel

@Composable
fun SharedMinimizedMusicPlayer(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onPlayerAreaClick: () -> Unit
) {
    val song by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val isFavorite by playerViewModel.isFavorite.collectAsStateWithLifecycle()

    song?.let { currentSong ->
        MinimizedMusicPlayer(
            title = currentSong.title,
            artist = currentSong.artist,
            coverUrl = currentSong.coverArtUri ?: "",
            isPlaying = isPlaying,
            isFavorite = isFavorite,
            onPlayPauseClick = playerViewModel::togglePlayPause,
            onFavoriteClick = playerViewModel::toggleFavorite,
            onPlayerClick = onPlayerAreaClick
        )
    }

}