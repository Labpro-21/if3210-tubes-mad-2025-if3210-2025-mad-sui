package com.vibecoder.purrytify.presentation.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vibecoder.purrytify.presentation.components.MusicCard

import com.vibecoder.purrytify.presentation.components.BottomNavigationBar
import com.vibecoder.purrytify.presentation.components.SmallMusicCard
import com.vibecoder.purrytify.presentation.theme.OffWhite
import com.vibecoder.purrytify.presentation.components.MinimizedMusicPlayer
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    navController: NavController,
    currentRoute: String,
    viewModel: HomeScreenViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        bottomBar = {
            Column {
                state.currentTrack?.let { track ->
                    MinimizedMusicPlayer(
                        title = track.title,
                        artist = track.artist,
//                        coverUrl = track.coverUrl,
                        coverUrl = "https://upload.wikimedia.org/wikipedia/en/3/39/The_Weeknd_-_Starboy.png",
                        isPlaying = state.isPlaying,
                        isFavorite = state.isFavorite,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onFavoriteClick = { viewModel.toggleFavorite() },
                        onPlayerClick = { /* Navigate to full player */ }
                    )
                }
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // New Songs
            item {
                SectionHeader(title = "New Songs")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.newSongs) { track ->
                        MusicCard(
                            title = track.title,
                            artist = track.artist,
                            coverUrl = track.coverUrl,
                            onClick = { viewModel.selectTrack(track) }
                        )
                    }
                }
            }

            // Recently Played Section
            item {
                SectionHeader(title = "Recently Played")
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.recentlyPlayed.forEach { track ->
                        SmallMusicCard(
                            title = track.title,
                            artist = track.artist,
                            coverUrl = track.coverUrl,
                            onClick = { viewModel.selectTrack(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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




data class Track(val id: String, val title: String, val artist: String, val coverUrl: String)

fun getDummyRecentlyPlayed(): List<Track> {
    return listOf(
        Track("1", "Bohemian Rhapsody", "Queen", "https://example.com/image1.jpg"),
        Track("2", "Blinding Lights", "The Weeknd", "https://example.com/image2.jpg"),
        Track("3", "Levitating", "Dua Lipa", "https://example.com/image3.jpg"),
        Track("4", "As It Was", "Harry Styles", "https://example.com/image4.jpg"),
        Track("5", "Bad Habit", "Steve Lacy", "https://example.com/image5.jpg")
    )
}

