package br.acerola.comic.common.ux.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.acerola.comic.common.navigation.Destination
import br.acerola.comic.common.ux.Acerola

private val navDestinations =
    listOf(
        Destination.HOME,
        Destination.HISTORY,
        Destination.CONFIG,
    )

@Composable
fun Acerola.Component.BottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier,
        windowInsets = NavigationBarDefaults.windowInsets,
        containerColor = NavigationBarDefaults.containerColor,
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
                        contentDescription = stringResource(destination.contentDescriptionRes),
                    )
                },
            )
        }
    }
}

@Composable
fun Acerola.Component.SideBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Insets: start (status bar top) + bottom (navigation bar), but NOT end
    // so the sidebar goes edge-to-edge on its own side like Spotify
    val sideBarInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Start + WindowInsetsSides.Vertical,
    )

    // Box externo: preenche toda a altura e aplica a cor de fundo (inclusive atrás da status bar e nav bar)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(sideBarInsets)
                .width(200.dp)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            navDestinations.forEach { destination ->
                val routeString = stringResource(id = destination.route)
                val isSelected = currentRoute == routeString
                val itemBackground = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(itemBackground)
                        .clickable {
                            if (currentRoute != routeString) {
                                navController.navigate(routeString) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.contentDescriptionRes),
                            tint = contentColor,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = destination.label),
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }
}
