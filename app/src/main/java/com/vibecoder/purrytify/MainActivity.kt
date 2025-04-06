package com.vibecoder.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.vibecoder.purrytify.presentation.components.BottomNavigationBar
import com.vibecoder.purrytify.presentation.features.auth.LoginScreen
import com.vibecoder.purrytify.presentation.features.home.HomeScreen
import com.vibecoder.purrytify.presentation.features.library.LibraryScreen
import com.vibecoder.purrytify.presentation.features.profile.ProfileScreen
import com.vibecoder.purrytify.presentation.features.player.FullScreenPlayerScreen
import com.vibecoder.purrytify.presentation.features.player.PlayerViewModel
import com.vibecoder.purrytify.presentation.features.shared.SharedMinimizedMusicPlayer
import com.vibecoder.purrytify.presentation.features.splash.SplashViewModel
import com.vibecoder.purrytify.presentation.theme.PurrytifyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

object AppDestinations {
    const val LOGIN_ROUTE = "login"
    const val HOME_ROUTE = "home"
    const val LIBRARY_ROUTE = "library"
    const val PROFILE_ROUTE = "profile"

}

// login has no bottom bar
val mainScaffoldRoutes = setOf(
    AppDestinations.HOME_ROUTE,
    AppDestinations.LIBRARY_ROUTE
    // AppDestinations.PROFILE_ROUTE
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            splashViewModel.isAuthenticated.value == null
        }

        enableEdgeToEdge()
        setContent {
            PurrytifyTheme {
                PurritifyApp(splashViewModel = splashViewModel)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurritifyApp(splashViewModel: SplashViewModel) {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()

    val isAuthenticated by splashViewModel.isAuthenticated.collectAsState()

    // after isAuthenticated is set, we can use it to determine the start destination
    val startDestination by remember(isAuthenticated) {
        derivedStateOf {
            when (isAuthenticated) {
                true -> AppDestinations.HOME_ROUTE
                false -> AppDestinations.LOGIN_ROUTE
                null -> AppDestinations.LOGIN_ROUTE
            }
        }
    }


    var showPlayerSheet by remember { mutableStateOf(false) }
    val playerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Observe the playerViewModel's UI events
    LaunchedEffect(playerViewModel, playerSheetState) {
        playerViewModel.uiEvents.collect { event ->
            when (event) {
                is PlayerViewModel.UiEvent.NavigateToFullScreenPlayer -> {
                    if (!playerSheetState.isVisible) {
                        showPlayerSheet = true

                    }
                }
                is PlayerViewModel.UiEvent.ShowSnackbar -> {
                    // TODO: Implement Snackbar
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    val showMainScaffoldBottomBar = currentRoute in mainScaffoldRoutes

    Scaffold(
        bottomBar = {
            if (showMainScaffoldBottomBar) {
                Column {
                    SharedMinimizedMusicPlayer(

                        onPlayerAreaClick = {
                            playerViewModel.onPlayerClicked()
                        }
                    )
                    BottomNavigationBar(navController = navController)
                }
            }

        }
    ) { innerPadding ->

        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        )
    }


    if (showPlayerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlayerSheet = false },
            sheetState = playerSheetState,
        ) {
            FullScreenPlayerScreen(
                playerViewModel = playerViewModel,
                onCollapse = {
                    scope.launch {
                        playerSheetState.hide()
                    }.invokeOnCompletion { if (!playerSheetState.isVisible) showPlayerSheet = false }
                }
            )
        }
    }
}


@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(AppDestinations.LOGIN_ROUTE) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppDestinations.HOME_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppDestinations.HOME_ROUTE) {
            HomeScreen()
        }
        composable(AppDestinations.LIBRARY_ROUTE) {
            LibraryScreen()
        }
        composable(AppDestinations.PROFILE_ROUTE){
            ProfileScreen(
                navController = navController
            )
        }
    }
}