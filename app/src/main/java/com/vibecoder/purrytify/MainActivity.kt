package com.vibecoder.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibecoder.purrytify.presentation.features.auth.LoginScreen
import com.vibecoder.purrytify.presentation.features.home.HomeScreen
import com.vibecoder.purrytify.presentation.features.library.LibraryScreen
import com.vibecoder.purrytify.presentation.features.profile.ProfileScreen
import com.vibecoder.purrytify.presentation.features.splash.SplashViewModel
import com.vibecoder.purrytify.presentation.theme.PurrytifyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


object AppDestinations {
    const val LOGIN_ROUTE = "login"
    const val HOME_ROUTE = "home"
    const val LIBRARY_ROUTE = "library"
    const val PROFILE_ROUTE = "profile"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            PurrytifyTheme {
                AppNavigation(isAuthenticatedFlow = splashViewModel.isAuthenticated)
            }
        }
    }
}

@Composable
fun AppNavigation(isAuthenticatedFlow: kotlinx.coroutines.flow.StateFlow<Boolean?>) {
    val navController = rememberNavController()
    val isAuthenticated by isAuthenticatedFlow.collectAsState()
//    val startDestination = if (isAuthenticated == false) {
//        AppDestinations.LOGIN_ROUTE
//    } else {
//        AppDestinations.HOME_ROUTE
//    }
    val startDestination = AppDestinations.HOME_ROUTE
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(AppDestinations.LOGIN_ROUTE) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppDestinations.HOME_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestinations.HOME_ROUTE) {
            HomeScreen(
                navController = navController,
            )
        }

       composable(AppDestinations.LIBRARY_ROUTE) {
           LibraryScreen(
               navController = navController,
           )
       }

        composable(AppDestinations.PROFILE_ROUTE){
            ProfileScreen(
                navController = navController
            )
        }
    }
}