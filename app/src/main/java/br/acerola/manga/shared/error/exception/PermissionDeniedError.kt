package br.acerola.manga.shared.error.exception

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import br.acerola.manga.R

class PermissionDeniedError(
    @param:StringRes override val title: Int = R.string.title_permission_denied_error,
    @param:StringRes override val description: Int = R.string.description_permission_denied_error,
    override val dismissButton: @Composable (() -> Unit)? = null,
    override val content: (@Composable () -> Unit)? = null
) : ApplicationException(content = content)