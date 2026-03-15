package br.acerola.manga.common.layout

import android.app.Activity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.acerola.manga.common.component.Acerola
import br.acerola.manga.common.component.TopBar
import br.acerola.manga.common.component.GlassButton
import br.acerola.manga.presentation.R
import br.acerola.manga.common.navigation.Destination

@Composable
fun NavigationTopBar(
    navController: NavHostController,
    extraActions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val titleText = Destination.entries.find {
        stringResource(id = it.route) == currentRoute
    }?.let {
        stringResource(id = it.label)
    } ?: "Acerola"

    Acerola.TopBar(
        title = titleText,
        navigationIcon = {
            Acerola.GlassButton(
                onClick = {
                    if (!navController.popBackStack()) {
                        (context as? Activity)?.finish()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.description_icon_navigation_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        },
        actions = {
            Row {
                extraActions()
            }
        }
    )
}
