package com.vibecoder.purrytify.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun NavigationRail(navController: NavController, modifier: Modifier = Modifier) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationRail(
            modifier = modifier.fillMaxHeight(),
            containerColor = Color.Black,
            contentColor = Color.White,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route

            NavigationRailItem(
                    icon = {
                        Icon(
                                painter = painterResource(id = item.iconResId),
                                contentDescription = stringResource(item.labelResId),
                                modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                                text = stringResource(item.labelResId),
                                style = MaterialTheme.typography.labelSmall
                        )
                    },
                    selected = selected,
                    colors =
                            NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = Color.Gray,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = Color.Gray,
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
