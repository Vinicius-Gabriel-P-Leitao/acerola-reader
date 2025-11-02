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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import br.acerola.manga.R
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopBar(navController: NavHostController, extraActions: @Composable RowScope.() -> Unit = {}) {
    val context = LocalContext.current

    TopAppBar(
        title = { Text(text = "Acerola") },
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