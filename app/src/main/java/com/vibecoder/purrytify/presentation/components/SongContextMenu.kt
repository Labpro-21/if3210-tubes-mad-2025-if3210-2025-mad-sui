package com.vibecoder.purrytify.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.presentation.features.player.PlayerViewModel
import com.vibecoder.purrytify.presentation.theme.DarkGray
import com.vibecoder.purrytify.presentation.theme.Red

@Composable
fun SongContextMenu(
        song: SongEntity,
        isOpen: Boolean,
        onDismiss: () -> Unit,
        songStatus: PlayerViewModel.SongStatus,
        onPlayPauseClick: () -> Unit,
        onToggleQueueClick: () -> Unit,
        onToggleFavorite: () -> Unit,
        onDelete: () -> Unit,
        onEdit: () -> Unit,
        isPlaying: Boolean = false
) {
    if (isOpen) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = DarkGray)
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    // Song Title and Artist
                    Column(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Play/Pause option
                    ContextMenuItem(
                            icon =
                                    if (isPlaying &&
                                                    songStatus ==
                                                            PlayerViewModel.SongStatus
                                                                    .CURRENTLY_PLAYING
                                    )
                                            Icons.Default.Pause
                                    else Icons.Default.PlayArrow,
                            text =
                                    if (isPlaying &&
                                                    songStatus ==
                                                            PlayerViewModel.SongStatus
                                                                    .CURRENTLY_PLAYING
                                    )
                                            "Pause"
                                    else "Play",
                            onClick = {
                                onPlayPauseClick()
                                onDismiss()
                            }
                    )

                    // Queue option
                    ContextMenuItem(
                            icon =
                                    if (songStatus == PlayerViewModel.SongStatus.IN_QUEUE)
                                            Icons.Default.RemoveFromQueue
                                    else Icons.Outlined.QueueMusic,
                            text =
                                    if (songStatus == PlayerViewModel.SongStatus.IN_QUEUE)
                                            "Remove from Queue"
                                    else "Add to Queue",
                            onClick = {
                                onToggleQueueClick()
                                onDismiss()
                            }
                    )

                    // Like option
                    ContextMenuItem(
                            icon =
                                    if (song.isLiked) Icons.Default.Favorite
                                    else Icons.Default.FavoriteBorder,
                            text =
                                    if (song.isLiked) "Remove from Liked Songs"
                                    else "Add to Liked Songs",
                            onClick = {
                                onToggleFavorite()
                                onDismiss()
                            }
                    )

                    // Edit option
                    val isCurrentlyPlaying =
                            songStatus == PlayerViewModel.SongStatus.CURRENTLY_PLAYING && isPlaying
                    ContextMenuItem(
                            icon = Icons.Default.Edit,
                            text =
                                    if (isCurrentlyPlaying) "Cannot Edit (Currently Playing)"
                                    else "Edit Song",
                            onClick = {
                                if (!isCurrentlyPlaying) {
                                    onEdit()
                                }
                                onDismiss()
                            },
                            tint = if (isCurrentlyPlaying) Color.Gray else Color.White,
                            enabled = !isCurrentlyPlaying
                    )

                    // Delete option
                    ContextMenuItem(
                            icon = Icons.Default.Delete,
                            text =
                                    if (isCurrentlyPlaying) "Cannot Delete (Currently Playing)"
                                    else "Delete Song",
                            onClick = {
                                if (!isCurrentlyPlaying) {
                                    onDelete()
                                }
                                onDismiss()
                            },
                            tint = if (isCurrentlyPlaying) Color.Gray else Color.Red,
                            enabled = !isCurrentlyPlaying
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
        icon: ImageVector,
        text: String,
        onClick: () -> Unit,
        tint: Color = Color.White,
        enabled: Boolean = true
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(enabled = enabled, onClick = onClick)
                            .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = text,
                tint = tint,
                modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = tint,
                modifier = if (enabled) Modifier else Modifier.alpha(0.5f)
        )
    }
}
