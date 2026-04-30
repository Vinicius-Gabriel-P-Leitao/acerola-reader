package br.acerola.comic.module.comic.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.Pagination
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.module.comic.component.ChapterItem
import br.acerola.comic.module.comic.component.VolumeCard
import br.acerola.comic.module.comic.component.VolumeHeader
import br.acerola.comic.util.normalizeChapter

@OptIn(ExperimentalFoundationApi::class)
fun Comic.Layout.chapterSection(
    scope: LazyListScope,
    chapters: ChapterDto,
    currentPage: Int,
    totalPages: Int,
    readChapters: List<String> = emptyList(),
    onChapterClick: (ChapterFileDto, ChapterFeedDto?) -> Unit,
    onToggleRead: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    showVolumeHeaders: Boolean = false,
    expandedVolumeIds: Set<Long> = emptySet(),
    onToggleVolumeExpanded: (Long) -> Unit = {},
    onLoadMoreVolume: (Long) -> Unit = {},
) {
    if (showVolumeHeaders && chapters.archive.volumeSections.isNotEmpty()) {
        chapters.archive.volumeSections.forEach { group ->
            scope.item(
                key = "volume_card_${group.volume.id}",
                contentType = "volume_card",
            ) {
                Comic.Component.VolumeCard(
                    group = group,
                    expanded = expandedVolumeIds.contains(group.volume.id),
                    readChapters = readChapters,
                    onToggleExpanded = { onToggleVolumeExpanded(group.volume.id) },
                    onLoadMore = { onLoadMoreVolume(group.volume.id) },
                    onChapterClick = { chapter -> onChapterClick(chapter, null) },
                    onToggleRead = onToggleRead,
                    remoteResolver = { chapterSort ->
                        chapters.remoteInfo?.items?.firstOrNull {
                            it.chapter.normalizeChapter() == chapterSort
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
        return
    }

    val volumeMap = chapters.archive.volumes.associateBy { it.id }
    val chapterCountByVolume =
        chapters.archive.items
            .mapNotNull { it.volumeId }
            .groupingBy { it }
            .eachCount()

    chapters.archive.items.forEachIndexed { index, archiveItem ->
        val volumeId = archiveItem.volumeId
        val previousVolumeId =
            chapters.archive.items
                .getOrNull(index - 1)
                ?.volumeId
        val shouldShowHeader = showVolumeHeaders && volumeId != null && volumeId != previousVolumeId

        if (shouldShowHeader) {
            val volumeDto = volumeMap[volumeId] ?: return@forEachIndexed
            scope.stickyHeader(key = "vol_$volumeId") {
                Comic.Component.VolumeHeader(
                    volume = volumeDto,
                    chapterCount = chapterCountByVolume[volumeId] ?: 0,
                )
            }
        }

        scope.item(
            key = archiveItem.id,
            contentType = "chapter",
        ) {
            val remoteItem: ChapterFeedDto? =
                chapters.remoteInfo?.items?.firstOrNull {
                    it.chapter.normalizeChapter() ==
                        archiveItem.chapterSort.normalizeChapter()
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

    if (totalPages > 1) {
        scope.item(
            key = "pagination_footer",
            contentType = "pagination",
        ) {
            Acerola.Component.Pagination(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = onPageChange,
            )
        }
    }
}
