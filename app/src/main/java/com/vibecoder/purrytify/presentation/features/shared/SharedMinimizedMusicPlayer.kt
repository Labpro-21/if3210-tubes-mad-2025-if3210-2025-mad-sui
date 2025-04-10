package com.vibecoder.purrytify.presentation.features.shared

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoder.purrytify.presentation.components.MinimizedMusicPlayer
import com.vibecoder.purrytify.presentation.features.player.PlayerViewModel

@Composable
fun SharedMinimizedMusicPlayer(
        playerViewModel: PlayerViewModel = hiltViewModel(),
        onPlayerAreaClick: () -> Unit
) {
    var shouldShowPlayer by remember { mutableStateOf(true) }

    val song by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val isFavorite by playerViewModel.isFavorite.collectAsStateWithLifecycle()

    LaunchedEffect(playerViewModel) {
        playerViewModel.uiEvents.collect { event ->
            when (event) {
                is PlayerViewModel.UiEvent.CurrentSongDeleted -> {
                    Log.d("SharedMinimizedPlayer", "Current song deleted, hiding player")
                    shouldShowPlayer = false
                }
                is PlayerViewModel.UiEvent.HideMinimizedPlayer -> {
                    shouldShowPlayer = false
                }
                else -> {}
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    playerViewModel.refreshPlayerState()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(song) { shouldShowPlayer = song != null }

    if (shouldShowPlayer && song != null) {
        MinimizedMusicPlayer(
                title = song!!.title,
                artist = song!!.artist,
                coverUrl = song!!.coverArtUri ?: "",
                isPlaying = isPlaying,
                isFavorite = isFavorite,
                onPlayPauseClick = playerViewModel::togglePlayPause,
                onFavoriteClick = playerViewModel::toggleFavorite,
                onPlayerClick = onPlayerAreaClick
        )
    }
}
