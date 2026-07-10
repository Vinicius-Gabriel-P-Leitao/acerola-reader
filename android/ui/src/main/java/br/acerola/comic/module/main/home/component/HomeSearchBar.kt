package br.acerola.comic.module.main.home.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.SearchBar
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R

@Composable
fun Main.Home.Component.HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    comics: List<Triple<ComicDto, ReadingHistoryDto?, Int>>,
    onComicClick: (ComicDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    Acerola.Component.SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        items = comics,
        placeholder = stringResource(R.string.label_home_search_placeholder),
        itemKey = { it.first.directory.id },
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp),
        itemContent = { (comic, history, chapterCount) ->
            Main.Home.Component.ComicSearchItem(
                comic = comic,
                chapterCount = chapterCount,
                onClick = { onComicClick(comic) },
            )
        },
    )
}
