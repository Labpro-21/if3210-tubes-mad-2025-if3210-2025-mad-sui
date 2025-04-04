package com.vibecoder.purrytify.presentation.features.library

import androidx.navigation.NavController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibecoder.purrytify.presentation.components.MinimizedMusicPlayer
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.compose.foundation.background
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

import com.vibecoder.purrytify.domain.model.Song
import com.vibecoder.purrytify.presentation.components.BottomNavigationBar
import androidx.compose.ui.graphics.Color
import com.vibecoder.purrytify.presentation.components.SongBottomSheet


@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val tabs = listOf("All", "Liked")
    val songs = viewModel.songs
    val currentSong = viewModel.currentPlayingSong
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isFavorite by viewModel.isCurrentSongFavorite.collectAsState()
    val selectedTab = viewModel.selectedTab
    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsState()


    Scaffold(
        bottomBar = {
            Column {
                currentSong?.let { song ->
                    MinimizedMusicPlayer(
                        title = song.title,
                        artist = song.artist,
                        coverUrl = song.coverUrl,
                        isPlaying = isPlaying,
                        isFavorite = isFavorite,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onFavoriteClick = { viewModel.toggleFavorite() },
                        onPlayerClick = { navController.navigate("player") }
                    )
                }
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                IconButton(onClick = { viewModel.showBottomSheet() }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to library")
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.4f)
                    .padding(vertical = 8.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { },
                    divider = { }
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.onTabSelected(index) },
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


            HorizontalDivider()


            AndroidView(
                factory = { context ->
                    RecyclerView(context).apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = SongAdapter(emptyList(), viewModel::onPlaySong)
                    }
                },
                update = { recyclerView ->

                    (recyclerView.adapter as? SongAdapter)?.updateSongs(songs)
                },
                modifier = Modifier.weight(1f)
            )
        }
        SongBottomSheet(
            isVisible = isBottomSheetVisible,
            onDismiss = { viewModel.hideBottomSheet() },
            title = "Upload Song",
            onSave = { title: String, artist: String -> {} }
        )
    }
}

