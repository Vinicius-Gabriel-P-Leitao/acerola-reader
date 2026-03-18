package br.acerola.manga.module.main.search.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.search.component.MangaResultCard
import br.acerola.manga.module.main.search.state.SearchAction
import br.acerola.manga.module.main.search.state.SearchUiState
import br.acerola.manga.presentation.R

@Composable
fun Main.Search.Layout.SearchLayout(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchInputHeader(uiState = uiState, onAction = onAction)
            SearchResultsList(uiState = uiState, onAction = onAction, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SearchInputHeader(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.title_search_screen),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { onAction(SearchAction.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.placeholder_search_mangadex)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.description_icon_search_submit)
                )
            },
            trailingIcon = {
                if (uiState.query.isNotBlank()) {
                    IconButton(onClick = { onAction(SearchAction.QueryChanged("")) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.description_icon_search_clear)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                keyboardController?.hide()
                onAction(SearchAction.Search)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.query.isNotBlank() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.description_icon_search_submit))
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            uiState.searchResults.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.searchResults, key = { it.mirrorId }) { manga ->
                        Main.Search.Component.MangaResultCard(
                            manga = manga,
                            onClick = { onAction(SearchAction.SelectManga(manga)) }
                        )
                    }
                }
            }
            !uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.label_search_empty_state),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
