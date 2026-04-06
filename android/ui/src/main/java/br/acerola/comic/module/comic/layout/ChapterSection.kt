package br.acerola.comic.module.comic.layout
import br.acerola.comic.ui.R

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.Pagination
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.module.comic.component.ChapterItem
import br.acerola.comic.util.normalizeChapter

fun Comic.Layout.ChapterSection(
    scope: LazyListScope,
    chapters: ChapterDto,
    currentPage: Int,
    totalPages: Int,
    readChapters: List<Long> = emptyList(),
    onChapterClick: (ChapterFileDto, ChapterFeedDto?) -> Unit,
    onToggleRead: (Long) -> Unit,
    onPageChange: (Int) -> Unit,
) {
    scope.items(
        items = chapters.archive.items,
        contentType = { "chapter" },
        key = { it.id },
    ) { archiveItem ->
        val remoteItem: ChapterFeedDto? = chapters.remoteInfo?.items?.firstOrNull { it.chapter.normalizeChapter() == archiveItem.chapterSort.normalizeChapter() }

        Comic.Component.ChapterItem(
            chapterFileDto = archiveItem,
            chapterRemoteInfoDto = remoteItem,
            isRead = readChapters.contains(archiveItem.id),
            onClick = { onChapterClick(archiveItem, remoteItem) },
            onToggleRead = { onToggleRead(archiveItem.id) },
            modifier = Modifier.padding(all = 4.dp)
        )
    }

    if (totalPages > 1) {
        scope.item(
            key = "pagination_footer",
            contentType = "pagination"
        ) {
            Acerola.Component.Pagination(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = onPageChange
            )
        }
    }
}
