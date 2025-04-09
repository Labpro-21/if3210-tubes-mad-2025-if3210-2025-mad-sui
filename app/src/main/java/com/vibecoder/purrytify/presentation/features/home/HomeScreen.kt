package com.vibecoder.purrytify.presentation.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.presentation.components.MusicCard
import com.vibecoder.purrytify.presentation.components.SmallMusicCard
import com.vibecoder.purrytify.presentation.components.SongContextMenu
import com.vibecoder.purrytify.presentation.features.player.PlayerViewModel

@Composable
fun HomeScreen(
        viewModel: HomeViewModel = hiltViewModel(),
        playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    // State for the context menu
    var selectedSong by remember { mutableStateOf<SongEntity?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedSongStatus by remember { mutableStateOf(PlayerViewModel.SongStatus.NOT_IN_QUEUE) }

    val queueSongs by playerViewModel.queueSongs.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.error != null -> {
                Text(
                        "Error: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            else -> {
                LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // New Songs
                    if (state.newSongs.isNotEmpty()) {
                        item {
                            SectionHeader(title = "New Songs")
                            LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(state.newSongs, key = { it.id }) { song ->
                                    val isCurrentSong = currentSong?.id == song.id
                                    val isSongPlaying = isCurrentSong && isPlaying

                                    MusicCard(
                                            title = song.title,
                                            artist = song.artist,
                                            coverUrl = song.coverArtUri ?: "",
                                            isCurrentSong = isCurrentSong,
                                            isPlaying = isSongPlaying,
                                            onClick = { viewModel.selectSong(song) },
                                            onPlayPauseClick = {},
                                            onMoreOptionsClick = {
                                                selectedSong = song
                                                selectedSongStatus =
                                                        PlayerViewModel.SongStatus.CURRENTLY_PLAYING
                                                showContextMenu = true
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Recently Played
                    if (state.recentlyPlayed.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Recently Played")
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.recentlyPlayed.take(5).forEach { song ->
                                    val isCurrentSong = currentSong?.id == song.id
                                    val isSongPlaying = isCurrentSong && isPlaying

                                    SmallMusicCard(
                                            title = song.title,
                                            artist = song.artist,
                                            coverUrl = song.coverArtUri ?: "",
                                            isCurrentSong = isCurrentSong,
                                            isPlaying = isSongPlaying,
                                            onClick = { viewModel.selectSong(song) },
                                            onPlayPauseClick = {
                                                if (isCurrentSong) {
                                                    viewModel.togglePlayPause()
                                                } else {
                                                    viewModel.selectSong(song)
                                                }
                                            },
                                            onMoreOptionsClick = {
                                                selectedSong = song
                                                selectedSongStatus =
                                                        PlayerViewModel.SongStatus.CURRENTLY_PLAYING
                                                showContextMenu = true
                                            }
                                    )
                                    HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color =
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                                            alpha = 0.5f
                                                    ),
                                            modifier = Modifier.padding(start = 64.dp)
                                    )
                                }
                            }
                        }
                    } else if (!state.isLoading) {
                        item {
                            SectionHeader(title = "Recently Played")
                            Text(
                                    "No recently played songs.",
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Context Menu
    selectedSong?.let { song ->
        SongContextMenu(
                song = song,
                isOpen = showContextMenu,
                onDismiss = { showContextMenu = false },
                songStatus = selectedSongStatus,
                onPlayPauseClick = {},
                onToggleQueueClick = {},
                onToggleFavorite = {},
                onDelete = {
                    // TODO: Implement this
                },
                onEdit = {
                    // TODO: Implement this
                }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
        )
    }
}
