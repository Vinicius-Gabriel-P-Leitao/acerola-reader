package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.manga.dto.ChapterDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.module.manga.component.ChapterItem
import br.acerola.manga.module.manga.component.PaginationFooter
import br.acerola.manga.util.normalizeChapter

fun Manga.Layout.ChapterSection(
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

        Manga.Component.ChapterItem(
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
            Manga.Component.PaginationFooter(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = onPageChange
            )
        }
    }
}
