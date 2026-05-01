package br.acerola.comic.module.comic.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.GlassButton
import br.acerola.comic.common.ux.component.GroupedHeroItem
import br.acerola.comic.common.ux.component.Pagination
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.ui.R
import br.acerola.comic.util.sort.normalizeSort

@Composable
fun Comic.Component.VolumeCard(
    group: VolumeChapterGroupDto,
    expanded: Boolean,
    readChapters: List<String>,
    onToggleExpanded: () -> Unit,
    onChapterClick: (ChapterFileDto) -> Unit,
    onToggleRead: (String) -> Unit,
    remoteResolver: (String) -> ChapterFeedDto?,
    currentPage: Int = 0,
    totalPages: Int = 1,
    onPageChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Acerola.Component.GroupedHeroItem(
        title = group.volume.name,
        description = stringResource(R.string.label_volume_card_description, group.loadedCount, group.totalChapters),
        icon = Icons.Default.LibraryBooks,
        iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier,
        onClick = onToggleExpanded,
        action = {
            Acerola.Component.GlassButton(
                onClick = onToggleExpanded,
                icon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        },
        nestedItem = {
            if (!expanded) return@GroupedHeroItem

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (group.volume.isSpecial) {
                    Text(
                        text = stringResource(id = R.string.label_volume_header_special),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                group.items.forEachIndexed { index, chapter ->
                    val remoteItem = remoteResolver(chapter.chapterSort.normalizeSort())

                    // Infinite Scroll Trigger for Volume
                    if (index >= group.items.size - 3 && currentPage < totalPages - 1) {
                        androidx.compose.runtime.LaunchedEffect(key1 = currentPage) {
                            onPageChange(currentPage + 1)
                        }
                    }

                    Comic.Component.ChapterItem(
                        chapterRemoteInfoDto = remoteItem,
                        chapterFileDto = chapter,
                        isRead = readChapters.contains(chapter.chapterSort),
                        onClick = { onChapterClick(chapter) },
                        onToggleRead = { onToggleRead(chapter.chapterSort) },
                    )
                }
            }
        },
    )
}
