package br.acerola.manga.module.main.search.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.SearchBar
import br.acerola.manga.module.download.DownloadActivity
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.search.component.MangaResultCard
import br.acerola.manga.module.main.search.state.SearchAction
import br.acerola.manga.module.main.search.state.SearchUiState
import br.acerola.manga.ui.R

import br.acerola.manga.module.main.search.component.DownloadQueueComponent

@Composable
fun Main.Search.Layout.SearchLayout(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!searchActive) {
                Text(
                    text = stringResource(R.string.title_search_screen),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }

            Acerola.Component.SearchBar(
                query = uiState.query,
                onQueryChange = { onAction(SearchAction.QueryChanged(it)) },
                onSearch = { 
                    onAction(SearchAction.Search)
                },
                active = searchActive,
                onActiveChange = { searchActive = it },
                isLoading = uiState.isLoading,
                items = uiState.searchResults,
                itemKey = { it.sources?.mangadex?.mangadexId ?: it.title },
                placeholder = stringResource(R.string.placeholder_search_mangadex),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (searchActive) 0.dp else 16.dp)
            ) { manga ->
                val context = LocalContext.current
                Main.Search.Component.MangaResultCard(
                    manga = manga,
                    onClick = { DownloadActivity.start(context, manga) }
                )
            }

            if (!searchActive) {
                DownloadQueueComponent(queue = uiState.downloadQueue)
            }

            if (!searchActive && uiState.searchResults.isEmpty() && !uiState.isLoading) {
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
