package br.acerola.manga.application

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import br.acerola.manga.shared.error.handler.GlobalErrorHandler
import br.acerola.manga.ui.common.component.Modal

@Composable
fun GlobalErrorRenderer() {
    val error = GlobalErrorHandler.errors.collectAsState(initial = null).value

    error?.let { exception ->
        Modal(
            show = true,
            onDismiss = { /* limpar erro global */ },
            title = exception.title ?: "Erro",
            confirmButtonContent = exception.confirmButton,
            dismissButtonContent = exception.dismissButton
        ) {
            exception.content?.invoke() ?: Text(text = exception.description)
        }
    }
}
