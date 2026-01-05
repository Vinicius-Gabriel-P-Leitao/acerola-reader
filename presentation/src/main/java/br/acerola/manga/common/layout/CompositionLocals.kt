package br.acerola.manga.common.layout

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

// TODO: Verificar o que é isso
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("Sem um proverdor para o SnackbarHostState")
}
