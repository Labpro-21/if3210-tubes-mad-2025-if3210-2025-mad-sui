package com.vibecoder.purrytify.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun SmallMusicCard(
        title: String,
        artist: String,
        coverUrl: String,
        isPlaying: Boolean = false,
        isCurrentSong: Boolean = false,
        onClick: () -> Unit,
        onPlayPauseClick: () -> Unit = {},
        onMoreOptionsClick: () -> Unit = {},
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Image(
                    painter = rememberAsyncImagePainter(model = coverUrl),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
            )

            // Show play/pause overlay if this is the current song
            if (isCurrentSong) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(
                                                MaterialTheme.colorScheme.background.copy(
                                                        alpha = 0.5f
                                                )
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                                imageVector =
                                        if (isPlaying) Icons.Default.Pause
                                        else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    color =
                            if (isCurrentSong) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground
            )
            Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                            if (isCurrentSong) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.secondary,
                    maxLines = 1
            )
        }
        // an icon to indicate the current song
        if (isCurrentSong && !isPlaying) {
            Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Current Song",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
            )
        }
        // options menu button
        IconButton(onClick = onMoreOptionsClick) {
            Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
