package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.module.manga.component.ChapterItem
import br.acerola.manga.module.manga.component.PaginationFooter

fun LazyListScope.chaptersSection(
    chapters: List<ChapterFileDto>,
    currentPage: Int,
    totalPages: Int,
    onChapterClick: (ChapterFileDto) -> Unit,
    onPageChange: (Int) -> Unit
) {
    items(
        contentType = { "chapter" },
        items = chapters,
        key = { it.id },
    ) { chapter ->
        ChapterItem(
            chapter = chapter,
            onClick = { onChapterClick(chapter) },
            modifier = Modifier.padding(all = 4.dp)
        )
    }

    if (totalPages > 1) {
        item(
            key = "pagination_footer",
            contentType = "pagination"
        ) {
            PaginationFooter(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = onPageChange
            )
        }
    }
}