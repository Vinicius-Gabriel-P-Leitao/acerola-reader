package br.acerola.comic.common.ux.theme.local
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbarHostState =
    staticCompositionLocalOf<SnackbarHostState> {
        error(
            "LocalSnackbarHostState não foi provido. Envolva o conteúdo com CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState).",
        )
    }
