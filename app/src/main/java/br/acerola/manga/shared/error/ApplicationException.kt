package br.acerola.manga.shared.error

import androidx.compose.runtime.Composable

sealed class ApplicationException(
    open val title: String? = null,
    open val description: String,
    open val confirmButton: (@Composable () -> Unit)? = null,
    open val dismissButton: (@Composable () -> Unit)? = null,
    open val content: (@Composable (() -> Unit))? = null
) : Exception(description)
