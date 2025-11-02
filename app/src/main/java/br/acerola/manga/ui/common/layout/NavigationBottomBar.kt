package br.acerola.manga.ui.common.layout

import android.app.Activity
import android.content.Intent
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
    val activity = context as? Activity

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        windowInsets = NavigationBarDefaults.windowInsets,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Destination.entries.forEachIndexed { index, destination ->
            val routeString = context.getString(destination.route)

            NavigationBarItem(
                selected = currentRoute == routeString,
                label = { Text(text = context.getString(destination.label)) },
                onClick = {
                    if (currentRoute != routeString) {
                        destination.activityClass?.java?.let { activityClass ->
                            val intent = Intent(context, activityClass).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }

                            context.startActivity(intent)
                            activity?.let {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    it.overrideActivityTransition(
                                        Activity.OVERRIDE_TRANSITION_OPEN, 0, 0
                                    )
                                } else {
                                    @Suppress("DEPRECATION") it.overridePendingTransition(0, 0)
                                }
                            }
                            activity?.finish()
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