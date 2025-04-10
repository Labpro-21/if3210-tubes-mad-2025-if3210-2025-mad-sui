package com.vibecoder.purrytify.presentation.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecoder.purrytify.presentation.theme.DarkGray
import com.vibecoder.purrytify.presentation.theme.Green
import com.vibecoder.purrytify.presentation.theme.Red
import com.vibecoder.purrytify.presentation.features.network.NetworkStateViewModel
import kotlinx.coroutines.delay

@Composable
fun NetworkAwareApp(
    networkViewModel: NetworkStateViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val isConnected by networkViewModel.isConnected.collectAsStateWithLifecycle()
    var showConnectedMessage by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // UI restore network reconnection state
    LaunchedEffect(isConnected) {
        if (isConnected) {
            if (showConnectedMessage) {
                delay(3000)
                showConnectedMessage = false
            }
        } else {
            showConnectedMessage = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        content()

        // Disconnected message
        AnimatedVisibility(
            visible = !isConnected,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .zIndex(10f)
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkGray,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Icon and message for no internet
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalWifiOff,
                            contentDescription = "No internet",
                            tint = Red,
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = "No internet connection",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Retry and settings buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { networkViewModel.refreshNetworkStatus() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Retry", style = MaterialTheme.typography.bodySmall)
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Connected message
        AnimatedVisibility(
            visible = isConnected && showConnectedMessage,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .zIndex(10f)
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkGray,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Connected",
                        tint = Green,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "Connected to the internet",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
