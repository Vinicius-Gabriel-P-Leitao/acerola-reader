package br.acerola.manga.ui.common.layout

import android.app.Activity
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.acerola.manga.shared.route.Destination

@Composable
fun NavigationBottomBar(navController: NavHostController) {
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        windowInsets = NavigationBarDefaults.windowInsets,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Destination.entries.forEachIndexed { index, destination ->
            if (destination === Destination.HOME || destination === Destination.HISTORY || destination === Destination.CONFIG) {
                val routeString = context.getString(destination.route)

                NavigationBarItem(
                    selected = currentRoute == routeString,
                    label = { Text(text = context.getString(destination.label)) },
                    onClick = {
                        if (currentRoute != routeString) {
                            navController.navigate(routeString) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = context.getString(destination.contentDescriptionRes)
                        )
                    },
                )
            }
        }
    }
}