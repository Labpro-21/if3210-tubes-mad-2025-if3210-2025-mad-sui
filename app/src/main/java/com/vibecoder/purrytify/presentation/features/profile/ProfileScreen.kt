package com.vibecoder.purrytify.presentation.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ModeEditOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibecoder.purrytify.R
import com.vibecoder.purrytify.presentation.components.MusicCard
import com.vibecoder.purrytify.presentation.components.BottomNavigationBar
import com.vibecoder.purrytify.presentation.components.MinimizedMusicPlayer
import com.vibecoder.purrytify.presentation.features.home.SectionHeader

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            Column {
                state.currentSong?.let { song ->
                    MinimizedMusicPlayer(
                        title = song.title,
                        artist = song.artist,
                        coverUrl = song.coverArtUri ?: "",
                        isPlaying = state.isPlaying,
                        isFavorite = song.isLiked,
                        onPlayPauseClick = viewModel::togglePlayPause,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onPlayerClick = { }
                    )
                }
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
//            Half-screen gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeightDp / 2)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF00667B), Color(0xFF002F38), Color.Black)
                        )
                    )
            )

            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        ProfileHeader(
                            username = state.userProfile.username,
                            location = state.userProfile.location,
                            profileImageUrl = state.userProfile.profileImageUrl,
                            onEditProfileClick = viewModel::navigateToEditProfile
                        )

                        ProfileStats(
                            songCount = state.userStats.songCount,
                            likedCount = state.userStats.likedCount,
                            listenedCount = state.userStats.listenedCount
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    username: String,
    location: String,
    profileImageUrl: String,
    onEditProfileClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AsyncImage(
                model = profileImageUrl,
                placeholder = painterResource(id = R.drawable.bron_bg),
                error = painterResource(id = R.drawable.bron_bg),
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )

            IconButton(
                onClick = onEditProfileClick,
                modifier = Modifier
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(28.dp)
                    .background(Color.White, shape = RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = username,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        Text(
            text = location,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onEditProfileClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text("Edit Profile", color = Color.White)
        }
    }
}

@Composable
fun ProfileStats(
    songCount: Int,
    likedCount: Int,
    listenedCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(count = songCount, label = "SONGS")
        StatItem(count = likedCount, label = "LIKED")
        StatItem(count = listenedCount, label = "LISTENED")
    }
}

@Composable
fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray
        )
    }
}
