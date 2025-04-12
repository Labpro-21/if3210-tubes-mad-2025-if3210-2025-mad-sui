package com.vibecoder.purrytify.presentation.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vibecoder.purrytify.R
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongBottomSheet(
        isVisible: Boolean,
        onDismiss: () -> Unit,
        title: String,
        artist: String,
        audioFileName: String?,
        coverFileName: String?,
        durationMillis: Long?,
        isLoading: Boolean,
        error: String?,
        isEditMode: Boolean = false,
        onTitleChange: (String) -> Unit,
        onArtistChange: (String) -> Unit,
        onSave: () -> Unit,
        onAudioSelect: () -> Unit,
        onCoverSelect: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val boxWidth = (screenWidth - 64.dp) / 2

    if (isVisible) {
        ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                dragHandle = {
                    Box(
                            modifier =
                                    Modifier.padding(vertical = 8.dp)
                                            .width(32.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.4f
                                                    )
                                            )
                    )
                }
        ) {
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .padding(
                                            bottom =
                                                    WindowInsets.navigationBars
                                                            .asPaddingValues()
                                                            .calculateBottomPadding()
                                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                        text = if (isEditMode) "Edit Song" else "Upload Song",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                )

                // Upload Boxes
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cover Upload Box
                    UploadBox(
                            label = coverFileName
                                            ?: if (isEditMode) "Change Cover" else "Upload Cover",
                            iconRes = R.drawable.ic_img_placeholder,
                            isSelected = coverFileName != null,
                            onClick = onCoverSelect,
                            modifier = Modifier.weight(1f)
                    )

                    // Audio Upload Box
                    UploadBox(
                            label = audioFileName
                                            ?: if (isEditMode) "Change Audio" else "Upload Audio",
                            iconRes = R.drawable.ic_song_placeholder,
                            isSelected = audioFileName != null,
                            onClick = onAudioSelect,
                            modifier = Modifier.weight(1f)
                    )
                }

                if (durationMillis != null && durationMillis > 0) {
                    val formattedDuration = formatDuration(durationMillis)
                    Text(
                            text = "Duration: $formattedDuration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 12.dp).align(Alignment.Start)
                    )
                }

                TextInputComponent(
                        value = title,
                        onValueChange = onTitleChange,
                        label = "Title",
                        placeholderText = "Enter song title",
                        modifier = Modifier.padding(bottom = 12.dp),
                        enabled = !isLoading
                )

                TextInputComponent(
                        value = artist,
                        onValueChange = onArtistChange,
                        label = "Artist",
                        placeholderText = "Enter artist name",
                        modifier = Modifier.padding(bottom = 16.dp),
                        enabled = !isLoading
                )

                if (error != null) {
                    Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = !isLoading,
                            text = "Cancel",
                    )

                    // Save Button
                    PrimaryButton(
                            text = if (isEditMode) "Update" else "Save",
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                            isLoading = isLoading,
                            enabled = !isLoading
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}
