package br.acerola.manga.shared.error.exception

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

sealed class ApplicationException(
    @param:StringRes open val title: Int? = null,
    @param:StringRes open val description: Int? = null,
    open val confirmButton: (@Composable () -> Unit)? = null,
    open val dismissButton: (@Composable () -> Unit)? = null,
    open val content: (@Composable (() -> Unit))? = null
) : Exception()
