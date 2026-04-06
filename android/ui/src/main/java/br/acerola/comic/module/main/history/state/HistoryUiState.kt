package br.acerola.comic.module.main.history.state
import br.acerola.comic.ui.R

import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.history.ReadingHistoryWithChapterDto

data class HistoryItemState(
    val manga: ComicDto,
    val history: ReadingHistoryWithChapterDto,
    val chapterCount: Int = 0
)

data class HistoryUiState(
    val items: List<HistoryItemState> = emptyList()
)
