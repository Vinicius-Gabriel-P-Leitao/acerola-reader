package br.acerola.manga.module.main.history.state

import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto

sealed interface HistoryAction {
    data class ClickManga(val manga: MangaDto) : HistoryAction
    data class ClickContinue(val manga: MangaDto, val history: ReadingHistoryWithChapterDto) : HistoryAction
}
