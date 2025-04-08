package com.vibecoder.purrytify.presentation.features.player

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay
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
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vibecoder.purrytify.R
import com.vibecoder.purrytify.presentation.theme.Black
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
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
        val isShuffleOn by playerViewModel.isShuffleOn.collectAsStateWithLifecycle()
        val repeatMode by playerViewModel.repeatModeIcon.collectAsStateWithLifecycle()

        var showQueueDialog by remember { mutableStateOf(false) }

        var dominantColor by remember { mutableStateOf(Color(0xFF550A1C)) }
        val context = LocalContext.current

        // Extract dominant color from album art
        LaunchedEffect(song?.coverArtUri) {
                song?.coverArtUri?.let { coverUri ->
                        try {
                                val loader = ImageLoader(context)
                                val request =
                                        ImageRequest.Builder(context)
                                                .data(coverUri)
                                                .allowHardware(false)
                                                .build()

                                val result = (loader.execute(request) as? SuccessResult)?.drawable
                                val bitmap = (result as? BitmapDrawable)?.bitmap

                                bitmap?.let {
                                        val palette = Palette.from(it).generate()
                                        dominantColor =
                                                Color(
                                                        palette.getDarkMutedColor(
                                                                palette.getDominantColor(
                                                                        0xFF550A1C.toInt()
                                                                )
                                                        )
                                                )
                                }
                        } catch (e: Exception) {}
                }
        }

        val gradientColors = listOf(dominantColor, Black)

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors = gradientColors,
                                                        startY = 0f,
                                                        endY = Float.POSITIVE_INFINITY
                                                )
                                )
        ) {
                // Main content
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(horizontal = 24.dp)
                                        .padding(top = 20.dp, bottom = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // Top navigation bar
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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

                                Text(
                                        text = "NOW PLAYING",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                )

                                IconButton(onClick = { /* Show options menu */}) {
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
                                modifier = Modifier.weight(1f).fillMaxWidth(0.85f).aspectRatio(1f),
                                contentAlignment = Alignment.Center
                        ) {
                                Image(
                                        painter =
                                                rememberAsyncImagePainter(
                                                        model = song?.coverArtUri
                                                                        ?: R.drawable
                                                                                .ic_song_placeholder
                                                ),
                                        contentDescription = "Album cover",
                                        modifier =
                                                Modifier.fillMaxSize()
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

                                        TooltipBox(
                                                positionProvider =
                                                        TooltipDefaults
                                                                .rememberPlainTooltipPositionProvider(),
                                                tooltip = {
                                                        PlainTooltip {
                                                                Text(
                                                                        if (isFavorite)
                                                                                "Remove from favorites"
                                                                        else "Add to favorites"
                                                                )
                                                        }
                                                },
                                                state = rememberTooltipState()
                                        ) {
                                                IconButton(
                                                        onClick = playerViewModel::toggleFavorite
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        if (isFavorite)
                                                                                Icons.Default
                                                                                        .Favorite
                                                                        else
                                                                                Icons.Default
                                                                                        .FavoriteBorder,
                                                                contentDescription =
                                                                        if (isFavorite)
                                                                                "Remove from favorites"
                                                                        else "Add to favorites",
                                                                tint =
                                                                        if (isFavorite) Color.White
                                                                        else
                                                                                Color.White.copy(
                                                                                        alpha = 0.7f
                                                                                )
                                                        )
                                                }
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
                                                onValueChange = { newPosition ->
                                                        playerViewModel.seekTo(newPosition.toLong())
                                                },
                                                valueRange =
                                                        0f..if (totalDuration > 0)
                                                                        totalDuration.toFloat()
                                                                else 100f,
                                                colors =
                                                        SliderDefaults.colors(
                                                                thumbColor = Color(0xFF32CD32),
                                                                activeTrackColor =
                                                                        Color(0xFF32CD32),
                                                                inactiveTrackColor =
                                                                        Color.White.copy(
                                                                                alpha = 0.3f
                                                                        )
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

                                Spacer(modifier = Modifier.height(16.dp))

                                // Shuffle and Repeat Controls
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // Shuffle button
                                        TooltipBox(
                                                positionProvider =
                                                        TooltipDefaults
                                                                .rememberPlainTooltipPositionProvider(),
                                                tooltip = {
                                                        PlainTooltip {
                                                                Text(
                                                                        "Shuffle: ${if (isShuffleOn) "On" else "Off"}"
                                                                )
                                                        }
                                                },
                                                state = rememberTooltipState()
                                        ) {
                                                IconButton(
                                                        onClick = playerViewModel::toggleShuffle
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Default.Shuffle,
                                                                contentDescription =
                                                                        "Toggle Shuffle",
                                                                tint =
                                                                        if (isShuffleOn)
                                                                                Color(0xFF32CD32)
                                                                        else
                                                                                Color.White.copy(
                                                                                        alpha = 0.7f
                                                                                ),
                                                                modifier = Modifier.size(24.dp)
                                                        )
                                                }
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        // Queue button
                                        TooltipBox(
                                                positionProvider =
                                                        TooltipDefaults
                                                                .rememberPlainTooltipPositionProvider(),
                                                tooltip = { PlainTooltip { Text("View Queue") } },
                                                state = rememberTooltipState()
                                        ) {
                                                IconButton(onClick = { showQueueDialog = true }) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.AutoMirrored.Outlined
                                                                                .QueueMusic,
                                                                contentDescription = "Show Queue",
                                                                tint =
                                                                        Color.White.copy(
                                                                                alpha = 0.7f
                                                                        ),
                                                                modifier = Modifier.size(24.dp)
                                                        )
                                                }
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        // Repeat button
                                        TooltipBox(
                                                positionProvider =
                                                        TooltipDefaults
                                                                .rememberPlainTooltipPositionProvider(),
                                                tooltip = {
                                                        PlainTooltip {
                                                                Text(
                                                                        when (repeatMode) {
                                                                                PlayerViewModel
                                                                                        .RepeatModeIcon
                                                                                        .NONE ->
                                                                                        "Repeat: Off"
                                                                                PlayerViewModel
                                                                                        .RepeatModeIcon
                                                                                        .ALL ->
                                                                                        "Repeat: All"
                                                                                PlayerViewModel
                                                                                        .RepeatModeIcon
                                                                                        .ONE ->
                                                                                        "Repeat: One"
                                                                        }
                                                                )
                                                        }
                                                },
                                                state = rememberTooltipState()
                                        ) {
                                                IconButton(
                                                        onClick = playerViewModel::cycleRepeatMode
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        when (repeatMode) {
                                                                                PlayerViewModel
                                                                                        .RepeatModeIcon
                                                                                        .NONE ->
                                                                                        Icons.Default
                                                                                                .Repeat
                                                                                PlayerViewModel
                                                                                        .RepeatModeIcon
                                                                                        .ALL ->
                                                                                        Icons.Rounded
                                                                                                .Replay
                                                                                PlayerViewModel
                                                                                        .RepeatModeIcon
                                                                                        .ONE ->
                                                                                        Icons.Rounded
                                                                                                .RepeatOne
                                                                        },
                                                                contentDescription =
                                                                        "Toggle Repeat Mode",
                                                                tint =
                                                                        if (repeatMode !=
                                                                                        PlayerViewModel
                                                                                                .RepeatModeIcon
                                                                                                .NONE
                                                                        )
                                                                                Color(0xFF32CD32)
                                                                        else
                                                                                Color.White.copy(
                                                                                        alpha = 0.7f
                                                                                ),
                                                                modifier = Modifier.size(24.dp)
                                                        )
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Playback controls
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // Previous button
                                        TooltipBox(
                                                positionProvider =
                                                        TooltipDefaults
                                                                .rememberPlainTooltipPositionProvider(),
                                                tooltip = { PlainTooltip { Text("Previous") } },
                                                state = rememberTooltipState()
                                        ) {
                                                IconButton(
                                                        onClick = playerViewModel::skipToPrevious,
                                                        modifier = Modifier.size(56.dp)
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Default.SkipPrevious,
                                                                contentDescription = "Previous",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(36.dp)
                                                        )
                                                }
                                        }

                                        // Play/Pause button
                                        TooltipBox(
                                                positionProvider =
                                                        TooltipDefaults
                                                                .rememberPlainTooltipPositionProvider(),
                                                tooltip = {
                                                        PlainTooltip {
                                                                Text(
                                                                        if (isPlaying) "Pause"
                                                                        else "Play"
                                                                )
                                                        }
                                                },
                                                state = rememberTooltipState()
                                        ) {
                                                IconButton(
                                                        onClick = playerViewModel::togglePlayPause,
                                                        modifier =
                                                                Modifier.size(72.dp)
                                                                        .clip(CircleShape)
                                                                        .background(
                                                                                Color(0xFF32CD32)
                                                                        )
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        if (isPlaying)
                                                                                Icons.Default.Pause
                                                                        else
                                                                                Icons.Default
                                                                                        .PlayArrow,
                                                                contentDescription =
                                                                        if (isPlaying) "Pause"
                                                                        else "Play",
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(40.dp)
                                                        )
                                                }
                                        }

                                        // Next button
                                        TooltipBox(
                                                positionProvider =
                                                        TooltipDefaults
                                                                .rememberPlainTooltipPositionProvider(),
                                                tooltip = { PlainTooltip { Text("Next") } },
                                                state = rememberTooltipState()
                                        ) {
                                                IconButton(
                                                        onClick = playerViewModel::skipToNext,
                                                        modifier = Modifier.size(56.dp)
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Default.SkipNext,
                                                                contentDescription = "Next",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(36.dp)
                                                        )
                                                }
                                        }
                                }
                        }
                }

                // Bottom Navigation Bar
                if (navController != null) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                                com.vibecoder.purrytify.presentation.components.BottomNavigationBar(
                                        navController = navController
                                )
                        }
                }
        }

        // Queue Dialog
        if (showQueueDialog) {
                QueueDialog(viewModel = playerViewModel, onDismiss = { showQueueDialog = false })
        }
}

@Composable
fun QueueDialog(viewModel: PlayerViewModel, onDismiss: () -> Unit) {
        val queueSongs by viewModel.queueSongs.collectAsStateWithLifecycle()
        val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Queue") },
                text = {
                        if (queueSongs.isEmpty()) {
                                Text("Queue is empty")
                        } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                        items(queueSongs) { song ->
                                                val isCurrentSong = song.id == currentSong?.id

                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 8.dp)
                                                                        .clickable {
                                                                                viewModel
                                                                                        .playQueueItemAt(
                                                                                                queueSongs
                                                                                                        .indexOf(
                                                                                                                song
                                                                                                        )
                                                                                        )
                                                                        },
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        // Song number or playing indicator
                                                        if (isCurrentSong) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default
                                                                                        .MusicNote,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFF32CD32),
                                                                        modifier =
                                                                                Modifier.size(24.dp)
                                                                )
                                                        } else {
                                                                Text(
                                                                        text =
                                                                                "${queueSongs.indexOf(song) + 1}",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        24.dp
                                                                                )
                                                                )
                                                        }

                                                        Spacer(modifier = Modifier.width(12.dp))

                                                        // Song info
                                                        Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                        text = song.title,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyLarge,
                                                                        color =
                                                                                if (isCurrentSong)
                                                                                        Color(
                                                                                                0xFF32CD32
                                                                                        )
                                                                                else
                                                                                        Color.Unspecified,
                                                                        maxLines = 1,
                                                                        overflow =
                                                                                TextOverflow
                                                                                        .Ellipsis
                                                                )
                                                                Text(
                                                                        text = song.artist,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        color =
                                                                                if (isCurrentSong)
                                                                                        Color(
                                                                                                        0xFF32CD32
                                                                                                )
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.8f
                                                                                                )
                                                                                else
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant,
                                                                        maxLines = 1,
                                                                        overflow =
                                                                                TextOverflow
                                                                                        .Ellipsis
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
                properties =
                        DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
}

private fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds =
                TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%d:%02d", minutes, seconds)
}
