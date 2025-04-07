package com.vibecoder.purrytify.presentation.features.player

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vibecoder.purrytify.AppDestinations
import com.vibecoder.purrytify.R
import com.vibecoder.purrytify.presentation.components.BottomNavigationBar
import com.vibecoder.purrytify.presentation.theme.Black
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun FullScreenPlayerScreen(
    playerViewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    navController: androidx.navigation.NavController? = null
) {
    val song by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val isFavorite by playerViewModel.isFavorite.collectAsStateWithLifecycle()
    val currentPosition by playerViewModel.currentPositionMs.collectAsStateWithLifecycle()
    val totalDuration by playerViewModel.totalDurationMs.collectAsStateWithLifecycle()

    var dominantColor by remember { mutableStateOf(Color(0xFF550A1C)) }
    val context = LocalContext.current

    // Extract dominant color from album art
    LaunchedEffect(song?.coverArtUri) {
        song?.coverArtUri?.let { coverUri ->
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverUri)
                    .allowHardware(false)
                    .build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap

                bitmap?.let {
                    val palette = Palette.from(it).generate()
                    dominantColor = Color(palette.getDarkMutedColor(palette.getDominantColor(0xFF550A1C.toInt())))
                }
            } catch (e: Exception) {
            }
        }
    }

    val gradientColors = listOf(dominantColor, Black)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // TODO: Add the option menu action here properly
                IconButton(onClick = { /* Show options menu */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Album artwork
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = song?.coverArtUri ?: R.drawable.ic_song_placeholder
                    ),
                    contentDescription = "Album cover",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Song info and controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Song title and favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song?.title ?: "Unknown Title",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = playerViewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Artist name
                Text(
                    text = song?.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { playerViewModel.seekTo(it.toLong()) },
                        valueRange = 0f..if (totalDuration > 0) totalDuration.toFloat() else 100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF32CD32),
                            activeTrackColor = Color(0xFF32CD32),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Text(
                            text = formatDuration(totalDuration),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playback controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button
                    IconButton(
                        onClick = playerViewModel::skipToPrevious,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play/Pause button
                    IconButton(
                        onClick = playerViewModel::togglePlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF32CD32))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Next button
                    IconButton(
                        onClick = playerViewModel::skipToNext,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar
        if (navController != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                BottomNavigationBar(navController = navController)
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%d:%02d", minutes, seconds)
}
