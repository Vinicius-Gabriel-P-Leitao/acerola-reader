package br.acerola.comic.module.comic.layout

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
            val isExpanded = activeVolumeId == group.volume.id
            val onToggleExpanded = { onSetActiveVolume(if (isExpanded) null else group.volume.id) }
            val remoteResolver: (String) -> ChapterFeedDto? = { chapterSort ->
                chapters.remoteInfo?.items?.firstOrNull { it.chapter.normalizeSort() == chapterSort }
            }

            scope.item(
                key = "vol_${group.volume.id}",
                contentType = "volume_card",
            ) {
                if (volumeViewMode == VolumeViewType.COVER_VOLUME) {
                    Comic.Component.CoverVolumeCard(
                        group = group,
                        expanded = isExpanded,
                        onToggleExpanded = onToggleExpanded,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                } else {
                    Comic.Component.VolumeCard(
                        group = group,
                        expanded = isExpanded,
                        onToggleExpanded = onToggleExpanded,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }

            if (isExpanded) {
                if (group.volume.isSpecial) {
                    scope.item(key = "vol_${group.volume.id}_special_header") {
                        androidx.compose.material3.Text(
                            text = stringResource(id = R.string.label_volume_header_special),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                }

                group.items.forEachIndexed { index, archiveItem ->
                    scope.item(
                        key = "vol_${group.volume.id}_ch_${archiveItem.id}",
                        contentType = "chapter",
                    ) {
                        val remoteItem = remoteResolver(archiveItem.chapterSort.normalizeSort())

                        // Infinite Scroll Trigger for Volume Chapters
                        if (index >= group.items.size - 5 && group.currentPage < group.totalPages - 1) {
                            LaunchedEffect(key1 = Unit) {
                                onLoadVolumeChaptersPage(group.volume.id, group.currentPage + 1)
                            }
                        }

                        Comic.Component.ChapterItem(
                            chapterFileDto = archiveItem,
                            chapterRemoteInfoDto = remoteItem,
                            isRead = readChapters.contains(archiveItem.chapterSort),
                            onClick = { onChapterClick(archiveItem, remoteItem) },
                            onToggleRead = { onToggleRead(archiveItem.chapterSort) },
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                        )
                    }
                }

                scope.item(key = "vol_${group.volume.id}_bottom_spacer") {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
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
                LaunchedEffect(key1 = Unit) {
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
