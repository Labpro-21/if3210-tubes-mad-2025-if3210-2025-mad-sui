package com.vibecoder.purrytify.presentation.features.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.presentation.theme.DarkGray
import com.vibecoder.purrytify.presentation.theme.Red

@Composable
fun PlayerOptionsMenu(
        song: SongEntity,
        isOpen: Boolean,
        onDismiss: () -> Unit,
        onAddToQueue: () -> Unit,
        onRemoveFromQueue: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        isInQueue: Boolean,
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

                    // Queue option
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clickable {
                                                if (isInQueue) onRemoveFromQueue()
                                                else onAddToQueue()
                                                onDismiss()
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                imageVector =
                                        if (isInQueue) Icons.Default.RemoveFromQueue
                                        else Icons.AutoMirrored.Outlined.QueueMusic,
                                contentDescription =
                                        if (isInQueue) "Remove from Queue" else "Add to Queue",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                                text = if (isInQueue) "Remove from Queue" else "Add to Queue",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                        )
                    }

                    // Edit option
                    val editEnabled = !isPlaying
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clickable(enabled = editEnabled) {
                                                if (editEnabled) {
                                                    onEdit()
                                                }
                                                onDismiss()
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Song",
                                tint = if (editEnabled) Color.White else Color.Gray,
                                modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                                text =
                                        if (editEnabled) "Edit Song"
                                        else "Cannot Edit (Currently Playing)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (editEnabled) Color.White else Color.Gray
                        )
                    }

                    // Delete option
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clickable(enabled = !isPlaying) {
                                                if (!isPlaying) {
                                                    onDelete()
                                                }
                                                onDismiss()
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Song",
                                tint = if (isPlaying) Color.Gray else Color.Red,
                                modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                                text =
                                        if (isPlaying) "Cannot Delete (Currently Playing)"
                                        else "Delete Song",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isPlaying) Color.Gray else Color.Red
                        )
                    }
                }
            }
        }
    }
}
