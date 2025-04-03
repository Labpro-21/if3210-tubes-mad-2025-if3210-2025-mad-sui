package com.vibecoder.purrytify.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // Import Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import com.vibecoder.purrytify.AppDestinations
import com.vibecoder.purrytify.R

sealed class NavigationItem(val route: String, val labelResId: Int, val iconResId: Int) {
    object Home : NavigationItem(
        AppDestinations.HOME_ROUTE,
        R.string.nav_home,
        R.drawable.ic_home
    )
    object Library : NavigationItem(
        "library",
//        AppDestinations.LIBRARY_ROUTE,
        R.string.nav_library,
        R.drawable.ic_library
    )
    object Profile : NavigationItem(
        "profile",
//        AppDestinations.PROFILE_ROUTE,
        R.string.nav_profile,
        R.drawable.ic_profile
    )
}

val bottomNavItems = listOf(
    NavigationItem.Home,
    NavigationItem.Library,
    NavigationItem.Profile
)
@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = stringResource(item.labelResId)
                    )
                },
                label = { Text(stringResource(item.labelResId)) },
                selected = currentRoute == item.route,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                ),
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}