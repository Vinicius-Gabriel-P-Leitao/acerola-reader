package br.acerola.comic.module.comic.template

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import br.acerola.comic.common.ux.tokens.SpacingTokens
import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.module.comic.component.ChapterItem
import br.acerola.comic.module.comic.component.CoverVolumeCard
import br.acerola.comic.module.comic.component.VolumeCard
import br.acerola.comic.util.sort.normalizeSort

fun Comic.Template.chapterSection(
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
    onLoadVolumeChaptersPage: (Long, Int) -> Unit = { _, _ -> },
    onExtractVolumeCover: (Long) -> Unit = {},
) {
    val useVolumeSections =
        (volumeViewMode == VolumeViewType.VOLUME || volumeViewMode == VolumeViewType.COVER_VOLUME) &&
            chapters.archive.volumeSections.isNotEmpty()

    if (useVolumeSections) {
        chapters.archive.volumeSections.forEach { group ->
            val isExpanded = activeVolumeId == group.volume.id
            val onToggleExpanded = { onSetActiveVolume(if (isExpanded) null else group.volume.id) }

            scope.item(
                key = "vol_${group.volume.id}",
                contentType = "volume_card",
            ) {
                if (volumeViewMode == VolumeViewType.COVER_VOLUME) {
                    Comic.Component.CoverVolumeCard(
                        group = group,
                        expanded = isExpanded,
                        onToggleExpanded = onToggleExpanded,
                        onExtractCover = { onExtractVolumeCover(group.volume.id) },
                        expandedContent = null,
                        modifier = Modifier.padding(horizontal = SpacingTokens.ExtraSmall, vertical = SpacingTokens.Small),
                    )
                } else {
                    Comic.Component.VolumeCard(
                        group = group,
                        expanded = isExpanded,
                        onToggleExpanded = onToggleExpanded,
                        expandedContent = null,
                        modifier = Modifier.padding(horizontal = SpacingTokens.ExtraSmall, vertical = SpacingTokens.Small),
                    )
                }
            }

            if (isExpanded) {
                group.items.forEachIndexed { index, chapter ->
                    val remoteInfo = chapters.remoteInfo?.items?.getOrNull(index)?.takeIf { it.id != -1L }
                    scope.item(
                        key = "vol_${group.volume.id}_ch_${chapter.id}",
                        contentType = "chapter_item",
                    ) {
                        Comic.Component.ChapterItem(
                            chapterFileDto = chapter,
                            chapterRemoteInfoDto = remoteInfo,
                            isRead = readChapters.contains(chapter.chapterSort),
                            onClick = { onChapterClick(chapter, remoteInfo) },
                            onToggleRead = { onToggleRead(chapter.chapterSort) },
                        )
                    }
                }

                if (group.hasMore) {
                    scope.item(key = "vol_load_more_${group.volume.id}") {
                        LaunchedEffect(group.currentPage) {
                            onLoadVolumeChaptersPage(group.volume.id, group.currentPage + 1)
                        }
                    }
                }
            }
        }
    } else {
        chapters.archive.items.forEachIndexed { index, chapter ->
            scope.item(
                key = "ch_${chapter.id}",
                contentType = "chapter_item",
            ) {
                val remoteInfo = chapters.remoteInfo?.items?.getOrNull(index)?.takeIf { it.id != -1L }
                Comic.Component.ChapterItem(
                    chapterFileDto = chapter,
                    chapterRemoteInfoDto = remoteInfo,
                    isRead = readChapters.contains(chapter.chapterSort),
                    onClick = { onChapterClick(chapter, remoteInfo) },
                    onToggleRead = { onToggleRead(chapter.chapterSort) },
                )
            }
        }

        if (currentPage + 1 < totalPages) {
            scope.item(key = "flat_load_more") {
                LaunchedEffect(currentPage) {
                    onPageChange(currentPage + 1)
                }
            }
        }
    }
}
