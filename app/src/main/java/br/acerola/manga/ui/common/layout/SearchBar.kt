package br.acerola.manga.ui.common.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun <T> SearchBar(
    items: List<T>,
    placeholder: String,
    itemKey: (T) -> Any,
    searchKey: (T) -> String,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    var query by remember { mutableStateOf(value = "") }
    var active by remember { mutableStateOf(value = false) }
    var filteredItems by remember { mutableStateOf(value = items) }

    LaunchedEffect(key1 = query, key2 = items) {
        withContext(context = Dispatchers.Default) {
            filteredItems = if (query.isEmpty()) {
                items
            } else {
                items.filter { item ->
                    searchKey(item).contains(other = query, ignoreCase = true)
                }
            }
        }
    }

    SearchBar(
        query = query,
        active = active,
        modifier = modifier,
        onSearch = { active = false },
        onQueryChange = { query = it },
        onActiveChange = { active = it },
        shape = RoundedCornerShape(size = 8.dp),
        placeholder = { Text(text = placeholder) },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (active) {
                IconButton(onClick = { query = ""; active = false }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.description_icon_search_close)
                    )
                }
            }
        },
    ) {
        if (filteredItems.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 16.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.description_text_search_no_results),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredItems,
                    key = { item -> itemKey(item) }
                ) { item ->
                    itemContent(item)
                }
            }
        }
    }
}