package br.acerola.manga.ui.common.layout

import android.app.Activity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.acerola.manga.R
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NavigationTopBar(navController: NavHostController, extraActions: @Composable RowScope.() -> Unit = {}) {
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    TopAppBar(
        title = {
            Destination.entries.find {
                context.getString(it.route) == currentRoute
            }?.let {
                context.getString(it.label)
            } ?: "Acerola"
        },
        actions = extraActions,
        navigationIcon = {
            SmartButton(type = ButtonType.ICON, onClick = {
                if (!navController.popBackStack()) {
                    (context as? Activity)?.finish()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = context.getString(R.string.description_icon_navigation_back)
                )
            }
        },
    )
}