package br.acerola.manga.module.main.history.state

import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto

data class HistoryItemState(
    val manga: MangaDto,
    val history: ReadingHistoryWithChapterDto
)

data class HistoryUiState(
    val items: List<HistoryItemState> = emptyList()
)
