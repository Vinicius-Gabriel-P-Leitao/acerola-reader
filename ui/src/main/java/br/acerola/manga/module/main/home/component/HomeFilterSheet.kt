package br.acerola.manga.module.main.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.config.preference.HomeSortPreference
import br.acerola.manga.config.preference.MangaSortType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.home.state.FilterSettings
import br.acerola.manga.pattern.MetadataSource
import br.acerola.manga.ui.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.title_home_filter_sheet),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Sort Section
            Text(
                text = stringResource(id = R.string.subtitle_home_sort),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )

            MangaSortType.entries.forEach { type ->
                val label = when (type) {
                    MangaSortType.TITLE -> stringResource(id = R.string.label_sort_title)
                    MangaSortType.CHAPTER_COUNT -> stringResource(id = R.string.label_sort_chapter_count)
                    MangaSortType.LAST_UPDATE -> stringResource(id = R.string.label_sort_last_update)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortChange(sortSettings.copy(type = type)) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterSettings.bookmarkCategoryId == null,
                    onClick = { onFilterChange(filterSettings.copy(bookmarkCategoryId = null)) },
                    label = { Text(stringResource(id = R.string.label_filter_all)) }
                )
                
                categories.forEach { category ->
                    FilterChip(
                        selected = filterSettings.bookmarkCategoryId == category.id,
                        onClick = { onFilterChange(filterSettings.copy(bookmarkCategoryId = category.id)) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.color))
                            )
                        }
                    )
                }
            }

            // Metadata Sources
            Text(
                text = stringResource(id = R.string.subtitle_home_filter_sources),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Opção Todos
                FilterChip(
                    selected = filterSettings.metadataSource == null,
                    onClick = { onFilterChange(filterSettings.copy(metadataSource = null)) },
                    label = { Text(stringResource(id = R.string.label_filter_all)) }
                )

                // Fontes do Enum
                MetadataSource.entries.forEach { source ->
                    val label = when (source) {
                        MetadataSource.MANGADEX -> stringResource(id = R.string.label_source_mangadex)
                        MetadataSource.ANILIST -> stringResource(id = R.string.label_source_anilist)
                        MetadataSource.COMIC_INFO -> stringResource(id = R.string.label_source_comic_info)
                    }
                    
                    FilterChip(
                        selected = filterSettings.metadataSource == source.displayName,
                        onClick = { onFilterChange(filterSettings.copy(metadataSource = source.displayName)) },
                        label = { Text(text = label) }
                    )
                }

                // Opção Sem metadados
                FilterChip(
                    selected = filterSettings.metadataSource == "NONE",
                    onClick = { onFilterChange(filterSettings.copy(metadataSource = "NONE")) },
                    label = { Text(stringResource(id = R.string.label_filter_none)) }
                )
            }
        }
    }
}
