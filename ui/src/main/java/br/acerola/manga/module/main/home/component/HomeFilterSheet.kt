package br.acerola.manga.module.main.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.config.preference.HomeSortPreference
import br.acerola.manga.config.preference.MangaSortType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.home.state.FilterSettings
import br.acerola.manga.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main.Home.Component.HomeFilterSheet(
    sortSettings: HomeSortPreference,
    filterSettings: FilterSettings,
    categories: List<CategoryDto>,
    onSortChange: (HomeSortPreference) -> Unit,
    onFilterChange: (FilterSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(id = R.string.title_home_filter_sheet),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Sort Section
            Text(
                text = stringResource(id = R.string.subtitle_home_sort),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )

            MangaSortType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortChange(sortSettings.copy(type = type)) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.name.replace("_", " "),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (sortSettings.type == type) {
                        IconButton(onClick = {
                            onSortChange(
                                sortSettings.copy(
                                    direction = if (sortSettings.direction == SortDirection.ASCENDING)
                                        SortDirection.DESCENDING else SortDirection.ASCENDING
                                )
                            )
                        }) {
                            Icon(
                                imageVector = if (sortSettings.direction == SortDirection.ASCENDING)
                                    Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Filter Section
            Text(
                text = stringResource(id = R.string.subtitle_home_filter),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.label_home_filter_show_hidden),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = filterSettings.showHidden,
                    onCheckedChange = { onFilterChange(filterSettings.copy(showHidden = it)) }
                )
            }

            // Categories (Bookmark Filter)
            Text(
                text = stringResource(id = R.string.subtitle_home_filter_categories),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                FilterChip(
                    selected = filterSettings.bookmarkCategoryId == null,
                    onClick = { onFilterChange(filterSettings.copy(bookmarkCategoryId = null)) },
                    label = { Text("All") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                categories.forEach { category ->
                    FilterChip(
                        selected = filterSettings.bookmarkCategoryId == category.id,
                        onClick = { onFilterChange(filterSettings.copy(bookmarkCategoryId = category.id)) },
                        label = { Text(category.name) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // Metadata Sources
            Text(
                text = stringResource(id = R.string.subtitle_home_filter_sources),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                listOf(null, "MANGADEX", "ANILIST", "COMIC_INFO", "NONE").forEach { source ->
                    FilterChip(
                        selected = filterSettings.metadataSource == source,
                        onClick = { onFilterChange(filterSettings.copy(metadataSource = source)) },
                        label = { Text(source ?: "All") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}
