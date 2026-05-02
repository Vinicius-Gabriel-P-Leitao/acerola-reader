package br.acerola.comic.module.comic.template

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.Pagination
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
    val remoteResolver: (String) -> ChapterFeedDto? = { chapterSort ->
        chapters.remoteInfo?.items?.firstOrNull { it.chapter.normalizeSort() == chapterSort }
    }

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
                val expandedContent: (@Composable () -> Unit)? =
                    if (isExpanded) {
                        {
                            Column {
                                group.items.forEach { chapter ->
                                    val remoteInfo = remoteResolver(chapter.chapterSort)
                                    Comic.Component.ChapterItem(
                                        chapterFileDto = chapter,
                                        chapterRemoteInfoDto = remoteInfo,
                                        isRead = readChapters.contains(chapter.chapterSort),
                                        onClick = { onChapterClick(chapter, remoteInfo) },
                                        onToggleRead = { onToggleRead(chapter.chapterSort) },
                                    )
                                }
                                if (group.hasMore) {
                                    LaunchedEffect(group.currentPage) {
                                        onLoadVolumeChaptersPage(group.volume.id, group.currentPage + 1)
                                    }
                                }
                            }
                        }
                    } else {
                        null
                    }

                if (volumeViewMode == VolumeViewType.COVER_VOLUME) {
                    Comic.Component.CoverVolumeCard(
                        group = group,
                        expanded = isExpanded,
                        onToggleExpanded = onToggleExpanded,
                        onExtractCover = { onExtractVolumeCover(group.volume.id) },
                        expandedContent = expandedContent,
                        modifier = Modifier.padding(horizontal = SpacingTokens.ExtraSmall, vertical = SpacingTokens.Small),
                    )
                } else {
                    Comic.Component.VolumeCard(
                        group = group,
                        expanded = isExpanded,
                        onToggleExpanded = onToggleExpanded,
                        expandedContent = expandedContent,
                        modifier = Modifier.padding(horizontal = SpacingTokens.ExtraSmall, vertical = SpacingTokens.Small),
                    )
                }
            }
        }
    } else {
        chapters.archive.items.forEach { chapter ->
            scope.item(
                key = "ch_${chapter.id}",
                contentType = "chapter_item",
            ) {
                val remoteInfo = remoteResolver(chapter.chapterSort)
                Comic.Component.ChapterItem(
                    chapterFileDto = chapter,
                    chapterRemoteInfoDto = remoteInfo,
                    isRead = readChapters.contains(chapter.chapterSort),
                    onClick = { onChapterClick(chapter, remoteInfo) },
                    onToggleRead = { onToggleRead(chapter.chapterSort) },
                )
            }
        }

        if (totalPages > 1) {
            scope.item {
                Acerola.Component.Pagination(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPageChange = onPageChange,
                )
            }
        }
    }
}
