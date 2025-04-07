package com.vibecoder.purrytify.presentation.features.library

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vibecoder.purrytify.presentation.components.BottomNavigationBar
import com.vibecoder.purrytify.presentation.components.MinimizedMusicPlayer
import com.vibecoder.purrytify.presentation.components.SongBottomSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    val tabs = listOf("All", "Liked")

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission)

    val songAdapter = remember {
        SongAdapter(emptyList(), libraryViewModel::onPlaySong)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = {
                if (permissionState.status.isGranted) {
                    libraryViewModel.showBottomSheet()
                } else {
                    permissionState.launchPermissionRequest()
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Song")
            }
        }



        Row(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.4f)
                .padding(vertical = 8.dp)
        ) {
            TabRow(
                selectedTabIndex = libraryState.selectedTab, modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { },
                indicator = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = libraryState.selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { libraryViewModel.onTabSelected(index) },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(32.dp)
                            .background(

                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
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
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            libraryState.libraryError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (libraryState.selectedTab == 0) "Your library is empty. Add some songs!"
                        else "You haven't liked any songs yet.",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {

                AndroidView(
                    factory = { context ->
                        RecyclerView(context).apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = songAdapter
                        }
                    },

                    update = { _ ->
                        songAdapter.updateSongs(libraryState.songs)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }


    if (libraryState.isBottomSheetVisible) {
        val bottomSheetViewModel: SongBottomSheetViewModel = hiltViewModel()
        val bottomSheetState by bottomSheetViewModel.state.collectAsStateWithLifecycle()

        //  contract
        val audioPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri: Uri? ->
                bottomSheetViewModel.setAudioFileUri(uri)
            }
        )


        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri: Uri? ->
                bottomSheetViewModel.setCoverImageUri(uri)
            }
        )

        LaunchedEffect(Unit) { /*  permission request  */ }
        LaunchedEffect(Unit) {
            bottomSheetViewModel.eventFlow.collect { event ->
                when (event) {
                    is SheetEvent.SaveSuccess -> libraryViewModel.hideBottomSheet(refreshList = true)
                    is SheetEvent.Dismiss -> libraryViewModel.hideBottomSheet(refreshList = true)
                }
            }
        }

        SongBottomSheet(
            isVisible = true,
            onDismiss = bottomSheetViewModel::dismiss,
            title = bottomSheetState.title,
            artist = bottomSheetState.artist,
            audioFileName = bottomSheetState.audioFileName,
            coverFileName = bottomSheetState.coverFileName,
            durationMillis = bottomSheetState.durationMs,
            isLoading = bottomSheetState.isLoading,
            error = bottomSheetState.error,
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
            onCoverSelect = {
                imagePickerLauncher.launch(arrayOf("image/*"))
            }
        )
    }

}
