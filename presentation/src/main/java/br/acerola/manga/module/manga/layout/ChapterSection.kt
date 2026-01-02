package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.ChapterPageDto
import br.acerola.manga.module.manga.component.ChapterItem
import br.acerola.manga.module.manga.component.PaginationFooter

fun LazyListScope.chaptersSection(
    chapterPage: ChapterPageDto?,
    textColor: Color,
    onChapterClick: (ChapterFileDto) -> Unit,
    onPageChange: (Int) -> Unit
) {
    if (chapterPage == null) return
    val totalPages = kotlin.math.ceil(x = chapterPage.total.toDouble() / chapterPage.pageSize).toInt()

    items(
        items = chapterPage.items,
        key = { it.id }
    ) { chapter ->
        ChapterItem(
            chapter = chapter,
            textColor = textColor,
            onClick = { onChapterClick(chapter) }
        )
    }

    item {
        PaginationFooter(
            currentPage = chapterPage.page,
            totalPages = totalPages,
            textColor = textColor,
            onPageChange = onPageChange
        )
    }
}
