package com.vibecoder.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge


import com.vibecoder.purrytify.presentation.features.auth.LoginScreen
import com.vibecoder.purrytify.presentation.theme.PurrytifyTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PurrytifyTheme {

                LoginScreen(
                    onLoginSuccess = {

                        println("Login Successful")
                    }

                )
            }
        }
    }
}
