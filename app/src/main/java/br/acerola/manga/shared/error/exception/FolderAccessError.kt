package br.acerola.manga.shared.error.exception

import androidx.compose.runtime.Composable

// TODO: Criar uma string
class FolderAccessError(
    override val title: String = "Erro de acesso a pasta.",
    override val description: String = "Não foi possível acessar a pasta.",
    override val confirmButton: @Composable (() -> Unit)? = null,
    override val dismissButton: @Composable (() -> Unit)? = null,
    override val content: (@Composable () -> Unit)? = null
) : ApplicationException(description = description, content = content)