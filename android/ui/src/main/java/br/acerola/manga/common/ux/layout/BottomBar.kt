package br.acerola.manga.common.ux.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.common.ux.Acerola

private val navDestinations = listOf(
    Destination.HOME,
    Destination.HISTORY,
    Destination.CONFIG,
)

@Composable
fun Acerola.Layout.BottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier,
        windowInsets = NavigationBarDefaults.windowInsets,
        containerColor = NavigationBarDefaults.containerColor
    ) {
        navDestinations.forEach { destination ->
            val routeString = stringResource(id = destination.route)

            NavigationBarItem(
                selected = currentRoute == routeString,
                label = { Text(text = stringResource(id = destination.label)) },
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
                        contentDescription = stringResource(destination.contentDescriptionRes)
                    )
                },
            )
        }
    }
}

@Composable
fun Acerola.Layout.SideBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        navDestinations.forEach { destination ->
            val routeString = stringResource(id = destination.route)

            NavigationRailItem(
                selected = currentRoute == routeString,
                label = { Text(text = stringResource(id = destination.label)) },
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
                        contentDescription = stringResource(destination.contentDescriptionRes)
                    )
                },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
