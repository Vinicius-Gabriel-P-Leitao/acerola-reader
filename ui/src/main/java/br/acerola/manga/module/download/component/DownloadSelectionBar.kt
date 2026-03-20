package br.acerola.manga.module.download.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.module.download.Download
import br.acerola.manga.module.download.state.DownloadAction
import br.acerola.manga.module.download.state.DownloadUiState
import br.acerola.manga.ui.R

@Composable
fun Download.Component.DownloadSelectionBar(
    uiState: DownloadUiState,
    onAction: (DownloadAction) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: count badge + loading indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${uiState.selectedChapterIds.size} / ${uiState.totalChapters} " +
                                stringResource(R.string.label_search_chapters),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (uiState.isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Row 2: full-width segmented button for page selection
            val pageAllSelected = uiState.chapters.isNotEmpty() &&
                    uiState.chapters.all { it.id in uiState.selectedChapterIds }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = pageAllSelected,
                    onClick = { onAction(DownloadAction.SelectAll) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2,
                        baseShape = RoundedCornerShape(8.dp)
                    ),
                    icon = {}
                ) {
                    Text(
                        text = stringResource(R.string.label_search_select_all),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                SegmentedButton(
                    selected = uiState.selectedChapterIds.isEmpty(),
                    onClick = { onAction(DownloadAction.DeselectAll) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2,
                        baseShape = RoundedCornerShape(8.dp)
                    ),
                    icon = {}
                ) {
                    Text(
                        text = stringResource(R.string.label_search_deselect_all),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Row 3: action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onAction(DownloadAction.DownloadAll) },
                    enabled = !uiState.isDownloading && uiState.totalChapters > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadForOffline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.label_download_all_chapters))
                }

                FilledTonalButton(
                    onClick = { onAction(DownloadAction.Download) },
                    enabled = uiState.selectedChapterIds.isNotEmpty() && !uiState.isDownloading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            R.string.label_download_selected,
                            uiState.selectedChapterIds.size
                        )
                    )
                }
            }
        }
    }
}
