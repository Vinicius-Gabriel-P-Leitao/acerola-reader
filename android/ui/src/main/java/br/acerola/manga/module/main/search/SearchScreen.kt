package br.acerola.manga.module.main.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.manga.common.ux.component.SnackbarVariant
import br.acerola.manga.common.ux.component.showSnackbar
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.search.layout.SearchLayout

@Composable
fun Main.Search.Layout.Screen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
        }
    }

    Main.Search.Layout.SearchLayout(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = Modifier.fillMaxSize()
    )
}
