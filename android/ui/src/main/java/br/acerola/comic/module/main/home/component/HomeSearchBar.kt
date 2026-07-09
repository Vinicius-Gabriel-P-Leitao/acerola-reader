package br.acerola.comic.module.main.home.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.SearchBar

@Composable
fun Main.Home.Component.HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    comics: List<ComicDto>,
    onComicClick: (ComicDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    render(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        items = comics,
        placeholder = stringResource(R.string.label_home_search_placeholder),
        modifier = modifier,
        onComicClick = onComicClick
    )
}

@Composable
private fun render(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<ComicDto>,
    placeholder: String,
    modifier: Modifier,
    onComicClick: (ComicDto) -> Unit,
) {
    Acerola.Component.SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        items = items,
        placeholder = placeholder,
        itemKey = { it.directory.id },
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp)
    ) { comic ->
        Main.Home.Component.ComicGridItem(
            comic = comic,
            history = null,
            chapterCount = 0,
            onShowActions = {},
            onClick = { onComicClick(comic) },
        )
    }
}
