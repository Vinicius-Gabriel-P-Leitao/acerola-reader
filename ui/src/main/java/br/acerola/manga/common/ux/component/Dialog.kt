package br.acerola.manga.common.ux.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Acerola.Component.Dialog(
    show: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    confirmButtonContent: (@Composable () -> Unit)? = null,
    dismissButtonContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            dismissButton = {
                dismissButtonContent?.invoke()
            },
            title = {
                title?.let {
                    Text(text = it)
                }
            },
            text = {
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    content()
                }
            },
            confirmButton = {
                confirmButtonContent?.invoke()
            },
        )
    }
}
