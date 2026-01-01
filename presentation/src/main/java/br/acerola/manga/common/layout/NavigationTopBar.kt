package br.acerola.manga.common.layout

import android.app.Activity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.acerola.manga.feature.R
import br.acerola.manga.common.component.ButtonType
import br.acerola.manga.common.component.SmartButton
import br.acerola.manga.common.navigation.Destination

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NavigationTopBar(navController: NavHostController, extraActions: @Composable RowScope.() -> Unit = {}) {
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    TopAppBar(
        actions = extraActions,
        title = {
            Destination.entries.find {
                stringResource(id = it.route) == currentRoute
            }?.let {
                stringResource(id = it.label)
            } ?: "Acerola"
        },
        navigationIcon = {
            SmartButton(type = ButtonType.ICON, onClick = {
                if (!navController.popBackStack()) {
                    (context as? Activity)?.finish()
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.description_icon_navigation_back)
                )
            }
        },
    )
}