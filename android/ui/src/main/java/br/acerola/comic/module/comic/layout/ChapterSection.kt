package br.acerola.comic.module.comic.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.Pagination
import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.module.comic.component.ChapterItem
import br.acerola.comic.module.comic.component.CoverVolumeCard
import br.acerola.comic.module.comic.component.VolumeCard
import br.acerola.comic.ui.R
import br.acerola.comic.util.sort.normalizeSort

fun Comic.Layout.chapterSection(
    scope: LazyListScope,
    chapters: ChapterDto,
    currentPage: Int,
    totalPages: Int,
    readChapters: List<String> = emptyList(),
    volumeViewMode: VolumeViewType = VolumeViewType.CHAPTER,
    activeVolumeId: Long? = null,
    onChapterClick: (ChapterFileDto, ChapterFeedDto?) -> Unit,
    onToggleRead: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onSetActiveVolume: (Long?) -> Unit = {},
    onUpdateVolumeView: (VolumeViewType) -> Unit = {},
    onLoadVolumeChaptersPage: (Long, Int) -> Unit = { _, _ -> },
) {
    val useVolumeSections =
        (volumeViewMode == VolumeViewType.VOLUME || volumeViewMode == VolumeViewType.COVER_VOLUME) &&
            chapters.archive.volumeSections.isNotEmpty()

    if (useVolumeSections) {
        chapters.archive.volumeSections.forEach { group ->
            scope.item(
                key = "vol_${group.volume.id}",
                contentType = "volume_card",
            ) {
                val isExpanded = activeVolumeId == group.volume.id
                val onToggleExpanded = { onSetActiveVolume(if (isExpanded) null else group.volume.id) }
                val remoteResolver: (String) -> ChapterFeedDto? = { chapterSort ->
                    chapters.remoteInfo?.items?.firstOrNull { it.chapter.normalizeSort() == chapterSort }
                }

                if (volumeViewMode == VolumeViewType.COVER_VOLUME) {
                    Comic.Component.CoverVolumeCard(
                        group = group,
                        expanded = isExpanded,
                        readChapters = readChapters,
                        onToggleExpanded = onToggleExpanded,
                        onChapterClick = { chapter -> onChapterClick(chapter, null) },
                        onToggleRead = onToggleRead,
                        remoteResolver = remoteResolver,
                        currentPage = group.currentPage,
                        totalPages = group.totalPages,
                        onPageChange = { page -> onLoadVolumeChaptersPage(group.volume.id, page) },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                } else {
                    Comic.Component.VolumeCard(
                        group = group,
                        expanded = isExpanded,
                        readChapters = readChapters,
                        onToggleExpanded = onToggleExpanded,
                        onChapterClick = { chapter -> onChapterClick(chapter, null) },
                        onToggleRead = onToggleRead,
                        remoteResolver = remoteResolver,
                        currentPage = group.currentPage,
                        totalPages = group.totalPages,
                        onPageChange = { page -> onLoadVolumeChaptersPage(group.volume.id, page) },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }
        }
        return
    }

    chapters.archive.items.forEachIndexed { index, archiveItem ->
        scope.item(
            key = "ch_${archiveItem.id}",
            contentType = "chapter",
        ) {
            val remoteItem: ChapterFeedDto? =
                chapters.remoteInfo?.items?.firstOrNull {
                    it.chapter.normalizeSort() == archiveItem.chapterSort.normalizeSort()
                }

            // Infinite Scroll Trigger
            if (index >= chapters.archive.items.size - 5 && currentPage < totalPages - 1) {
                androidx.compose.runtime.LaunchedEffect(key1 = Unit) {
                    onPageChange(currentPage + 1)
                }
            }

            Comic.Component.ChapterItem(
                chapterFileDto = archiveItem,
                chapterRemoteInfoDto = remoteItem,
                isRead = readChapters.contains(archiveItem.chapterSort),
                onClick = { onChapterClick(archiveItem, remoteItem) },
                onToggleRead = { onToggleRead(archiveItem.chapterSort) },
                modifier = Modifier.padding(all = 4.dp),
            )
        }
    }
}
