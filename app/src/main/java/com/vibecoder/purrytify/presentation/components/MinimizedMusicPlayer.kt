package com.vibecoder.purrytify.presentation.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vibecoder.purrytify.presentation.theme.DarkGray
import com.vibecoder.purrytify.presentation.theme.Green
import com.vibecoder.purrytify.presentation.theme.PurrytifyTheme

@Composable
fun MinimizedMusicPlayer(
        title: String,
        artist: String,
        coverUrl: String,
        isPlaying: Boolean,
        isFavorite: Boolean = false,
        onPlayPauseClick: () -> Unit,
        onFavoriteClick: () -> Unit = {},
        onPlayerClick: () -> Unit
) {
    var dominantColor by remember { mutableStateOf(DarkGray) }
    var textColor by remember { mutableStateOf(Color.White) }
    val context = LocalContext.current

    LaunchedEffect(coverUrl) {
        if (coverUrl.isNotEmpty()) {
            try {
                val loader = ImageLoader(context)
                val request =
                        ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap

                bitmap?.let {
                    val palette = Palette.from(it).generate()

                    dominantColor = Color(palette.getDarkMutedColor(DarkGray.toArgb()))

                    val vibrantSwatch = palette.getVibrantSwatch()
                    if (vibrantSwatch != null &&
                                    hasGoodContrast(dominantColor, Color(vibrantSwatch.rgb))
                    ) {
                        textColor = Color(vibrantSwatch.rgb)
                    }
                }
            } catch (e: Exception) {
                dominantColor = DarkGray
                textColor = Color.White
            }
        }
    }

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .height(64.dp)
                            .clickable(onClick = onPlayerClick)
                            .background(dominantColor),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
                modifier = Modifier.padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                    painter = rememberAsyncImagePainter(model = coverUrl, onSuccess = {}),
                    contentDescription = title,
                    modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        color = textColor
                )
                Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f),
                        maxLines = 1
                )
            }
        }

        Row(
                modifier = Modifier.padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                        imageVector =
                                if (isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                        contentDescription =
                                if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Green else textColor
                )
            }
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                        imageVector =
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Green
                )
            }
        }
    }
}

private fun hasGoodContrast(background: Color, text: Color): Boolean {
    val bgLuminance = (0.299 * background.red + 0.587 * background.green + 0.114 * background.blue)
    val textLuminance = (0.299 * text.red + 0.587 * text.green + 0.114 * text.blue)
    return kotlin.math.abs(bgLuminance - textLuminance) > 0.5
}

@Preview(showBackground = true)
@Composable
fun MinimizedMusicPlayerPreview() {
    var isPlaying by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    PurrytifyTheme {
        MinimizedMusicPlayer(
                title = "Starboy",
                artist = "The Weeknd",
                coverUrl =
                        "https://upload.wikimedia.org/wikipedia/en/3/39/The_Weeknd_-_Starboy.png",
                isPlaying = isPlaying,
                isFavorite = isFavorite,
                onPlayPauseClick = { isPlaying = !isPlaying },
                onFavoriteClick = { isFavorite = !isFavorite },
                onPlayerClick = {}
        )
    }
}
