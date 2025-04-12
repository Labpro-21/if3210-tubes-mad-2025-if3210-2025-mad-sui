package com.vibecoder.purrytify.presentation.features.home

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.presentation.components.MusicCard
import com.vibecoder.purrytify.presentation.components.SmallMusicCard
import com.vibecoder.purrytify.presentation.components.SongBottomSheet
import com.vibecoder.purrytify.presentation.components.SongContextMenu
import com.vibecoder.purrytify.presentation.features.library.SheetEvent
import com.vibecoder.purrytify.presentation.features.library.SongBottomSheetViewModel
import com.vibecoder.purrytify.presentation.features.player.PlayerViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

    // State for the context menu
    var selectedSong by remember { mutableStateOf<SongEntity?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedSongStatus by remember { mutableStateOf(PlayerViewModel.SongStatus.NOT_IN_QUEUE) }

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
                                        onPlayPauseClick = {
                                            if (isCurrentSong) {
                                                playerViewModel.togglePlayPause()
                                            } else {
                                                viewModel.selectSong(song)
                                            }
                                        },
                                        onMoreOptionsClick = {
                                            selectedSong = song
                                            selectedSongStatus =
                                                playerViewModel.checkSongStatus(song)
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
                                                playerViewModel.togglePlayPause()
                                            } else {
                                                viewModel.selectSong(song)
                                            }
                                        },
                                        onMoreOptionsClick = {
                                            selectedSong = song
                                            selectedSongStatus =
                                                playerViewModel.checkSongStatus(song)
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
    selectedSong?.let { songg ->
        val song = state.newSongs.find { it.id == songg.id } 
        ?: state.recentlyPlayed.find { it.id == songg.id }
        ?: songg
        SongContextMenu(
            song = song,
            isOpen = showContextMenu,
            onDismiss = { showContextMenu = false },
            songStatus = selectedSongStatus,
            isPlaying = isPlaying && song.id == currentSong?.id,
            onPlayPauseClick = {
                if (song.id == currentSong?.id) {
                    playerViewModel.togglePlayPause()
                } else {
                    viewModel.selectSong(song)
                }
            },
            onToggleQueueClick = { playerViewModel.toggleQueueStatus(song) },
            onToggleFavorite = { viewModel.toggleLikeStatus(song) },
            onDelete = { viewModel.deleteSong(song) },
            onEdit = { viewModel.showEditSongDialog(song) }
        )
    }

    // Edit dialog - Using safe let to fix the smart cast issue
    if (state.showEditDialog) {
        state.songToEdit?.let { songToEdit ->
            val editViewModel: SongBottomSheetViewModel = hiltViewModel()
            val editState by editViewModel.state.collectAsStateWithLifecycle()

            // Set edit mode for the song using safe variable
            LaunchedEffect(songToEdit) {
                editViewModel.setEditMode(songToEdit)
            }

            // File picker contracts
            val audioPickerLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri: Uri? -> editViewModel.setAudioFileUri(uri) }
                )

            val imagePickerLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri: Uri? -> editViewModel.setCoverImageUri(uri) }
                )

            LaunchedEffect(Unit) {
                editViewModel.eventFlow.collect { event ->
                    when (event) {
                        is SheetEvent.SaveSuccess ->
                            viewModel.hideEditSongDialog(refreshList = true)
                        is SheetEvent.Dismiss ->
                            viewModel.hideEditSongDialog(refreshList = false)
                    }
                }
            }

            // Permission state for file access
            val permission =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            val permissionState = rememberPermissionState(permission)

            SongBottomSheet(
                isVisible = true,
                onDismiss = {
                    editViewModel.dismiss()
                    viewModel.hideEditSongDialog()
                },
                title = editState.title,
                artist = editState.artist,
                audioFileName = editState.audioFileName,
                coverFileName = editState.coverFileName,
                durationMillis = editState.durationMs,
                isLoading = editState.isLoading,
                error = editState.error,
                isEditMode = editState.isEditMode,
                onTitleChange = editViewModel::updateTitle,
                onArtistChange = editViewModel::updateArtist,
                onSave = editViewModel::saveSong,
                onAudioSelect = {
                    if (permissionState.status.isGranted) {
                        audioPickerLauncher.launch(arrayOf("audio/*"))
                    } else {
                        permissionState.launchPermissionRequest()
                    }
                },
                onCoverSelect = { imagePickerLauncher.launch(arrayOf("image/*")) }
            )
        }
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