package com.vibecoder.purrytify.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vibecoder.purrytify.R 
import com.vibecoder.purrytify.presentation.theme.DarkGray
import com.vibecoder.purrytify.presentation.theme.Gray 
import com.vibecoder.purrytify.presentation.theme.OutlineColor
import com.vibecoder.purrytify.presentation.theme.White 


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    onSave: (title: String, artist: String) -> Unit,
) {
    if (isVisible) {
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
        )

       
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            var titleState by remember { mutableStateOf("") }
            var artistState by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp), 
                horizontalAlignment = Alignment.CenterHorizontally 
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    UploadBox(
                        label = "Upload Photo",
                        iconRes = R.drawable.ic_img_placeholder,

                        onClick = { /* TODO: Handle photo upload */ }
                    )
                    UploadBox(
                        label = "Upload File",
                        iconRes = R.drawable.ic_song_placeholder,

                        onClick = { /* TODO: Handle file upload */ }
                    )
                }

                
                TextInputComponent(
                    value = titleState,
                    onValueChange = { titleState = it },
                    label = "Title",
                    placeholderText = "Title",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TextInputComponent(
                    value = artistState,
                    onValueChange = { artistState = it },
                    label = "Artist",
                    placeholderText = "Artist",
                    modifier = Modifier.padding(bottom = 32.dp) 
                )

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp) 
                ) {
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f) 
                            .height(48.dp),
                        shape = MaterialTheme.shapes.large, 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gray, 
                            contentColor = White 
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                  
                    PrimaryButton(
                        text = "Save",
                        onClick = { onSave(titleState, artistState) },
                        modifier = Modifier.weight(1f) 
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
