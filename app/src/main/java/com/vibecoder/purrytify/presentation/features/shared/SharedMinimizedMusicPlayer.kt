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
    var forceUpdate by remember { mutableStateOf(0) } /

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
                is PlayerViewModel.UiEvent.ShowSnackbar -> {
                    if (event.message.contains("updated") || event.message.contains("edited")) {
                        Log.d("SharedMinimizedPlayer", "Song updated, forcing UI refresh")
                        forceUpdate++
                    }
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
                    Log.d("SharedMinimizedPlayer", "Lifecycle resumed, refreshing player state")
                    playerViewModel.refreshPlayerState()
                    forceUpdate++ // Increment to trigger recomposition on resume
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(song) {
        shouldShowPlayer = song != null
        Log.d("SharedMinimizedPlayer", "Song changed, shouldShowPlayer = $shouldShowPlayer")
    }

    key(song?.id, forceUpdate) {
        if (shouldShowPlayer && song != null) {
            Log.d("SharedMinimizedPlayer", "Rendering player with song: ${song!!.title}")
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
}
