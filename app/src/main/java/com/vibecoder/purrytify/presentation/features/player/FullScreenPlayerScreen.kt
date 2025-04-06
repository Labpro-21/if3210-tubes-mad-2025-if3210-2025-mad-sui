package com.vibecoder.purrytify.presentation.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FullScreenPlayerScreen(
    playerViewModel: PlayerViewModel,
    onCollapse: () -> Unit
) {
    val song by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

    // MOCK UI
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        IconButton(onClick = onCollapse) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse Player")
        }
        Spacer(Modifier.height(20.dp))

        Text(
            "Full Screen Player (Mock)",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        Text("Now Playing:", style = MaterialTheme.typography.titleMedium)
        Text(song?.title ?: "---", style = MaterialTheme.typography.titleLarge)
        Text(song?.artist ?: "---", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(30.dp))
        Button(onClick = playerViewModel::togglePlayPause) {
            Text(if (isPlaying) "Pause" else "Play")
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = playerViewModel::toggleFavorite) {
            Text("Toggle Like")
        }
    }
}