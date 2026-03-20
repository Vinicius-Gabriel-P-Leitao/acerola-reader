package br.acerola.manga.common.ux.layout

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.common.ux.Acerola

@Composable
fun Acerola.Layout.BottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier.clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        windowInsets = NavigationBarDefaults.windowInsets,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Destination.entries.forEachIndexed { _, destination ->
            if (destination === Destination.HOME || destination === Destination.HISTORY || destination === Destination.SEARCH || destination === Destination.CONFIG) {
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
}
