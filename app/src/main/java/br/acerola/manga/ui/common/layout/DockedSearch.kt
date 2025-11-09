package br.acerola.manga.ui.common.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun <T> DockedSearch(
    items: List<T>,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar...",
    onItemSelected: (T) -> Unit
) {
    var query by remember { mutableStateOf(value = "") }
    var active by remember { mutableStateOf(value = false) }

    DockedSearchBar(
        query = query,
        active = active,
        onSearch = { active = false },
        onQueryChange = { query = it },
        onActiveChange = { active = it },
        shape = RoundedCornerShape(size = 6.dp),
        placeholder = { Text(text = placeholder) },
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 6.dp),
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (active) {
                IconButton(onClick = { query = ""; active = false }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        },
    ) {
        val filteredItems = remember(key1 = query, key2 = items) {
            items.filter { item ->
                itemLabel(item).contains(other = query, ignoreCase = true)
            }
        }

        if (filteredItems.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
            ) {
                Text(text = "Nenhum resultado encontrado", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filteredItems) { item ->
                    ListItem(
                        headlineContent = { Text(text = itemLabel(item)) },
                        modifier = Modifier
                            .clickable {
                                onItemSelected(item)
                                query = itemLabel(item)
                                active = false
                            }
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}