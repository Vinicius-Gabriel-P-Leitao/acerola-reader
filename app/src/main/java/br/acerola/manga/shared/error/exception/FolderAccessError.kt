package br.acerola.manga.shared.error.exception

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import br.acerola.manga.R

class FolderAccessError(
    @param:StringRes override val title: Int = R.string.title_folder_access_error,
    @param:StringRes override val description: Int = R.string.description_folder_access_error,
    override val confirmButton: @Composable (() -> Unit)? = null,
    override val dismissButton: @Composable (() -> Unit)? = null,
    override val content: (@Composable () -> Unit)? = null
) : ApplicationException(content = content)