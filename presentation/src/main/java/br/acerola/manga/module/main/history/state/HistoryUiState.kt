package br.acerola.manga.module.main.history.state

import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto

data class HistoryUiState(
    val manga: MangaDto,
    val history: ReadingHistoryWithChapterDto
)