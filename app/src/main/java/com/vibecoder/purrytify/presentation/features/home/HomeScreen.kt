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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Check if we're in landscape mode
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Adjust padding and layout based on orientation
    val horizontalPadding = if (isLandscape) 24.dp else 16.dp
    val bottomPadding = if (isLandscape) 16.dp else 80.dp

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
                // Main screen content
                LazyColumn(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(
                                                start = horizontalPadding,
                                                end = if (isLandscape) 8.dp else horizontalPadding
                                        ),
                        contentPadding = PaddingValues(top = 16.dp, bottom = bottomPadding),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // New Songs Section
                    if (state.newSongs.isNotEmpty()) {
                        item {
                            Text(
                                    text = "New songs for You",
                                    style =
                                            MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 24.sp
                                            ),
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 16.dp)
                            )

                            LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(end = 16.dp)
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

                    // Recently Played Section
                    item {
                        Text(
                                text = "Recently played",
                                style =
                                        MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 24.sp
                                        ),
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 16.dp)
                        )

                        if (state.recentlyPlayed.isEmpty()) {
                            Text(
                                    "No recently played songs.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(end = 16.dp)
                            ) {
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

                                    if (song != state.recentlyPlayed.take(5).lastOrNull()) {
                                        HorizontalDivider(
                                                thickness = 0.5.dp,
                                                color = Color.DarkGray,
                                                modifier = Modifier.padding(start = 64.dp)
                                        )
                                    }
                                }
                            }
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

    // Edit dialog
    if (state.showEditDialog) {
        state.songToEdit?.let { songToEdit ->
            val editViewModel: SongBottomSheetViewModel = hiltViewModel()
            val editState by editViewModel.state.collectAsStateWithLifecycle()

            // Set edit mode for the song
            LaunchedEffect(songToEdit) { editViewModel.setEditMode(songToEdit) }

            // File picker contracts
            val audioPickerLauncher =
                    rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument(),
                            onResult = { uri: Uri? ->
                                uri?.let { editViewModel.setAudioFileUri(it) }
                            }
                    )

            val imagePickerLauncher =
                    rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument(),
                            onResult = { uri: Uri? ->
                                uri?.let { editViewModel.setCoverImageUri(it) }
                            }
                    )

            // Observe events from the edit view model
            LaunchedEffect(Unit) {
                editViewModel.eventFlow.collect { event ->
                    when (event) {
                        is SheetEvent.SaveSuccess -> {
                            viewModel.hideEditSongDialog(refreshList = true)
                        }
                        is SheetEvent.Dismiss -> {
                            viewModel.hideEditSongDialog(refreshList = false)
                        }
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
