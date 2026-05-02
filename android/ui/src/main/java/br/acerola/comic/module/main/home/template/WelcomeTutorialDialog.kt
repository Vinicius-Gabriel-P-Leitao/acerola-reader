package br.acerola.comic.module.main.home.template

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R

// FIXME: Aplicar isso
@Composable
fun Main.Home.Component.WelcomeTutorialDialog(
    onDismiss: () -> Unit,
    onNavigateToConfig: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.title_tutorial_setup)) },
        text = { Text(text = stringResource(id = R.string.description_tutorial_setup)) },
        confirmButton = {
            Button(onClick = {
                onNavigateToConfig()
                onDismiss()
            }) {
                Text(text = stringResource(id = R.string.action_configure_library))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        },
    )
}
