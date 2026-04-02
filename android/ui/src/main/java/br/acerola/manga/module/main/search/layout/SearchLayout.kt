package br.acerola.manga.module.main.search.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.SearchBar
import br.acerola.manga.module.download.DownloadActivity
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.search.component.DownloadQueueComponent
import br.acerola.manga.module.main.search.component.MangaResultCard
import br.acerola.manga.module.main.search.state.SearchAction
import br.acerola.manga.module.main.search.state.SearchUiState
import br.acerola.manga.ui.R

@Composable
fun Main.Search.Layout.SearchLayout(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!searchExpanded) {
                    DownloadQueueComponent(
                        queue = uiState.downloadQueue,
                        modifier = Modifier.padding(top = 72.dp, bottom = 8.dp)
                    )
                }

                if (!searchExpanded && uiState.searchResults.isEmpty() && !uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {
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

            Acerola.Component.SearchBar(
                query = uiState.query,
                onQueryChange = { onAction(SearchAction.QueryChanged(it)) }, onSearch = { onAction(SearchAction.Search) }, expanded = searchExpanded,
                onExpandedChange = { searchExpanded = it }, onBackClick = { searchExpanded = false },
                isLoading = uiState.isLoading,
                items = uiState.searchResults,
                itemKey = { it.sources?.mangadex?.mangadexId ?: it.title },
                placeholder = stringResource(R.string.placeholder_search_mangadex),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                contentPadding = innerPadding
            ) { manga ->
                val context = LocalContext.current
                Main.Search.Component.MangaResultCard(
                    manga = manga,
                    onClick = { DownloadActivity.start(context, manga) }
                )
            }
        }
    }
}
