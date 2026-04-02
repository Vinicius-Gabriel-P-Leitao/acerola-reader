package br.acerola.manga.module.manga.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.config.preference.ChapterSortPreferenceData
import br.acerola.manga.config.preference.ChapterSortType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Manga.Component.ChapterSortSheet(
    sortSettings: ChapterSortPreferenceData,
    onSortChange: (ChapterSortPreferenceData) -> Unit,
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
                text = stringResource(id = R.string.title_chapter_sort_sheet),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ChapterSortType.entries.forEach { type ->
                val label = when (type) {
                    ChapterSortType.NUMBER -> stringResource(id = R.string.label_sort_number)
                    ChapterSortType.LAST_UPDATE -> stringResource(id = R.string.label_sort_last_update)
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
        }
    }
}
