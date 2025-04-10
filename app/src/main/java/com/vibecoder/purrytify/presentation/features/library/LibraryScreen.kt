package com.vibecoder.purrytify.presentation.features.library

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.presentation.components.SmallMusicCard
import com.vibecoder.purrytify.presentation.components.SongBottomSheet
import com.vibecoder.purrytify.presentation.components.SongContextMenu
import com.vibecoder.purrytify.presentation.features.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
        libraryViewModel: LibraryViewModel = hiltViewModel(),
        playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val tabs = listOf("All", "Liked")
    var showSearchBar by remember { mutableStateOf(false) }

    var selectedSong by remember { mutableStateOf<SongEntity?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
    val permissionState = rememberPermissionState(permission)

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp)
    ) {
        // Search Bar
        if (showSearchBar) {
            SearchBar(
                    query = libraryState.searchQuery,
                    onQueryChange = libraryViewModel::setSearchQuery,
                    onClose = {
                        showSearchBar = false
                        // Clear search query when closing search bar
                        libraryViewModel.setSearchQuery("")
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        } else {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                        text = "Your Library",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                )

                Row {
                    // Search icon
                    IconButton(onClick = { showSearchBar = true }) {
                        Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Add icon
                    IconButton(
                            onClick = {
                                if (permissionState.status.isGranted) {
                                    libraryViewModel.showBottomSheet()
                                } else {
                                    permissionState.launchPermissionRequest()
                                }
                            }
                    ) { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Song") }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(fraction = 0.4f).padding(vertical = 8.dp)) {
            TabRow(
                    selectedTabIndex = libraryState.selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    indicator = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = libraryState.selectedTab == index
                    Tab(
                            selected = isSelected,
                            onClick = { libraryViewModel.onTabSelected(index) },
                            modifier =
                                    Modifier.padding(horizontal = 4.dp)
                                            .height(32.dp)
                                            .background(
                                                    color =
                                                            if (isSelected)
                                                                    MaterialTheme.colorScheme
                                                                            .primary
                                                            else Color.DarkGray,
                                                    shape = RoundedCornerShape(16.dp)
                                            ),
                            selectedContentColor = Color.Black,
                            unselectedContentColor = Color.White
                    ) {
                        Text(
                                text = title,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

        when {
            libraryState.isLoadingSongs -> {
                Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            libraryState.libraryError != null -> {
                Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            "Error: ${libraryState.libraryError}",
                            color = MaterialTheme.colorScheme.error
                    )
                }
            }
            libraryState.songs.isEmpty() && !libraryState.isLoadingSongs -> {
                Box(
                        modifier = Modifier.fillMaxSize().weight(1f).padding(16.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            if (libraryState.searchQuery.isNotEmpty()) "No songs match your search."
                            else if (libraryState.selectedTab == 0)
                                    "Your library is empty. Add some songs!"
                            else "You haven't liked any songs yet.",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                // Using Compose LazyColumn
                LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items = libraryState.songs, key = { song -> song.id }) { song ->
                        val isCurrentSong = song.id == currentSong?.id
                        val isSongPlaying = isCurrentSong && isPlaying

                        SmallMusicCard(
                                title = song.title,
                                artist = song.artist,
                                coverUrl = song.coverArtUri ?: "",
                                isPlaying = isSongPlaying,
                                isCurrentSong = isCurrentSong,
                                onClick = { libraryViewModel.onPlaySong(song) },
                                onPlayPauseClick = {
                                    if (isCurrentSong) {
                                        libraryViewModel.togglePlayPause()
                                    } else {
                                        libraryViewModel.onPlaySong(song)
                                    }
                                },
                                onMoreOptionsClick = {
                                    selectedSong = song
                                    showContextMenu = true
                                }
                        )

                        HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // Song Context Menu
    selectedSong?.let { song ->
        SongContextMenu(
                song = song,
                isOpen = showContextMenu,
                onDismiss = { showContextMenu = false },
                songStatus = playerViewModel.checkSongStatus(song),
                isPlaying = isPlaying && song.id == currentSong?.id,
                onPlayPauseClick = {
                    if (song.id == currentSong?.id) {
                        playerViewModel.togglePlayPause()
                    } else {
                        libraryViewModel.onPlaySong(song)
                    }
                },
                onToggleQueueClick = { playerViewModel.toggleQueueStatus(song) },
                onToggleFavorite = { libraryViewModel.toggleFavoriteForSong(song) },
                onDelete = { libraryViewModel.deleteSong(song.id) },
                onEdit = { libraryViewModel.showEditDialog(song) }
        )
    }

    //  Add/Edit Song
    if (libraryState.isBottomSheetVisible || libraryState.showEditDialog) {
        val bottomSheetViewModel: SongBottomSheetViewModel = hiltViewModel()
        val bottomSheetState by bottomSheetViewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(libraryState.songToEdit) {
            val song = libraryState.songToEdit
            if (libraryState.showEditDialog && song != null) {
                bottomSheetViewModel.setEditMode(song)
            }
        }

        // File picker contracts
        val audioPickerLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                        onResult = { uri: Uri? -> bottomSheetViewModel.setAudioFileUri(uri) }
                )

        val imagePickerLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                        onResult = { uri: Uri? -> bottomSheetViewModel.setCoverImageUri(uri) }
                )

        LaunchedEffect(Unit) {
            bottomSheetViewModel.eventFlow.collect { event ->
                when (event) {
                    is SheetEvent.SaveSuccess ->
                            if (libraryState.showEditDialog) {
                                libraryViewModel.hideEditDialog(refreshList = true)
                            } else {
                                libraryViewModel.hideBottomSheet(refreshList = true)
                            }
                    is SheetEvent.Dismiss ->
                            if (libraryState.showEditDialog) {
                                libraryViewModel.hideEditDialog(refreshList = false)
                            } else {
                                libraryViewModel.hideBottomSheet(refreshList = false)
                            }
                }
            }
        }

        SongBottomSheet(
                isVisible = true,
                onDismiss = {
                    if (libraryState.showEditDialog) {
                        bottomSheetViewModel.dismiss()
                        libraryViewModel.hideEditDialog()
                    } else {
                        bottomSheetViewModel.dismiss()
                        libraryViewModel.hideBottomSheet()
                    }
                },
                title = bottomSheetState.title,
                artist = bottomSheetState.artist,
                audioFileName = bottomSheetState.audioFileName,
                coverFileName = bottomSheetState.coverFileName,
                durationMillis = bottomSheetState.durationMs,
                isLoading = bottomSheetState.isLoading,
                error = bottomSheetState.error,
                isEditMode = bottomSheetState.isEditMode,
                onTitleChange = bottomSheetViewModel::updateTitle,
                onArtistChange = bottomSheetViewModel::updateArtist,
                onSave = bottomSheetViewModel::saveSong,
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

@Composable
fun SearchBar(
        query: String,
        onQueryChange: (String) -> Unit,
        onClose: () -> Unit,
        modifier: Modifier = Modifier
) {
    TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
            placeholder = { Text("Search for songs or artists") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                IconButton(
                        onClick = {
                            if (query.isNotEmpty()) {
                                onQueryChange("")
                            } else {
                                onClose()
                            }
                        }
                ) { Icon(imageVector = Icons.Default.Close, contentDescription = "Clear") }
            },
            singleLine = true,
            colors =
                    TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                    )
    )
}
