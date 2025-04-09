package com.vibecoder.purrytify.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vibecoder.purrytify.presentation.theme.Black
import com.vibecoder.purrytify.presentation.theme.OffWhite
import com.vibecoder.purrytify.presentation.theme.White

@Composable
fun MusicCard(
        title: String,
        artist: String,
        coverUrl: String,
        isCurrentSong: Boolean = false,
        isPlaying: Boolean = false,
        onClick: () -> Unit,
        onPlayPauseClick: () -> Unit = {},
        onMoreOptionsClick: () -> Unit = {},
        modifier: Modifier = Modifier
) {
    Card(
            modifier = modifier.width(150.dp).clickable(onClick = onClick).padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.small,
            elevation =
                    CardDefaults.cardElevation(
                            defaultElevation = if (isCurrentSong) 4.dp else 0.dp
                    ),
            colors =
                    CardDefaults.cardColors(
                            containerColor = if (isCurrentSong) Color(0xFF112211) else Black
                    )
    ) {
        Box {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    AsyncImage(
                            model = coverUrl,
                            contentDescription = "$title by $artist",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                    )

                    // Show play/pause overlay when it's the current song
                    if (isCurrentSong) {
                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                    onClick = onPlayPauseClick,
                                    modifier =
                                            Modifier.size(48.dp)
                                                    .clip(RoundedCornerShape(24.dp))
                                                    .background(Color(0xFF32CD32))
                            ) {
                                Icon(
                                        imageVector =
                                                if (isPlaying) Icons.Default.Pause
                                                else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    //  more options
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(onClick = onMoreOptionsClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // New song indicator in the bottom left corner
                    if (!isCurrentSong) {
                        Box(
                                modifier =
                                        Modifier.align(Alignment.BottomStart)
                                                .padding(8.dp)
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF32CD32).copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "New Song",
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isCurrentSong) Color(0xFF32CD32) else White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                            text = artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                    if (isCurrentSong) Color(0xFF32CD32).copy(alpha = 0.8f)
                                    else OffWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
