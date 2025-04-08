package com.vibecoder.purrytify.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.presentation.theme.DarkGray

@Composable
fun SongContextMenu(
        song: SongEntity,
        isOpen: Boolean,
        onDismiss: () -> Unit,
        onPlayNow: () -> Unit,
        onAddToQueue: () -> Unit,
        onToggleFavorite: () -> Unit,
        onDelete: () -> Unit,
        onEdit: () -> Unit,
) {
  if (isOpen) {
    Dialog(onDismissRequest = onDismiss) {
      Card(
              modifier = Modifier.fillMaxWidth().wrapContentHeight(),
              shape = MaterialTheme.shapes.medium,
              colors = CardDefaults.cardColors(containerColor = DarkGray)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Song Info
          Row(
                  modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                  verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImageWithFallback(
                    url = song.coverArtUri,
                    contentDescription = "Song Cover",
                    modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                      text = song.title,
                      style = MaterialTheme.typography.titleMedium,
                      color = Color.White,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
              )
              Text(
                      text = song.artist,
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.White.copy(alpha = 0.7f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
              )
            }
          }

          Divider(color = Color.White.copy(alpha = 0.1f))

          // Action buttons
          MenuOption(
                  icon = Icons.Default.PlayArrow,
                  text = "Play Now",
                  onClick = {
                    onPlayNow()
                    onDismiss()
                  }
          )

          MenuOption(
                  icon = Icons.Outlined.QueueMusic,
                  text = "Add to Queue",
                  onClick = {
                    onAddToQueue()
                    onDismiss()
                  }
          )

          MenuOption(
                  icon = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                  text = if (song.isLiked) "Remove from Liked Songs" else "Add to Liked Songs",
                  onClick = {
                    onToggleFavorite()
                    onDismiss()
                  }
          )

          MenuOption(
                  icon = Icons.Default.Edit,
                  text = "Edit Song",
                  onClick = {
                    onEdit()
                    onDismiss()
                  }
          )

          MenuOption(
                  icon = Icons.Default.Delete,
                  text = "Delete Song",
                  onClick = {
                    onDelete()
                    onDismiss()
                  },
                  tint = Color.Red
          )
        }
      }
    }
  }
}

@Composable
private fun MenuOption(
        icon: ImageVector,
        text: String,
        onClick: () -> Unit,
        tint: Color = Color.White
) {
  Row(
          modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(24.dp)
    )

    Spacer(modifier = Modifier.width(16.dp))

    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = tint)
  }
}

@Composable
private fun AsyncImageWithFallback(
        url: String?,
        contentDescription: String?,
        modifier: Modifier = Modifier
) {
  if (url.isNullOrEmpty()) {
    Box(modifier = modifier.background(Color.Gray), contentAlignment = Alignment.Center) {
      Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = contentDescription,
              tint = Color.White
      )
    }
  } else {
    androidx.compose.foundation.Image(
            painter = coil.compose.rememberAsyncImagePainter(model = url),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
  }
}
