package br.acerola.manga.common.ux.theme.local

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

// TODO: Tratar melhor esse erro
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("Sem um proverdor para o SnackbarHostState")
}
