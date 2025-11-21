package br.acerola.manga.shared.error

import androidx.compose.runtime.Composable

// TODO: Criar uma string
class PermissionDeniedError(
    override val title: String = "Erro de permissão.",
    override val description: String = "Permissão negada.",
    override val dismissButton: @Composable (() -> Unit)? = null,
    override val content: (@Composable () -> Unit)? = null
) : ApplicationException(description = description, content = content)