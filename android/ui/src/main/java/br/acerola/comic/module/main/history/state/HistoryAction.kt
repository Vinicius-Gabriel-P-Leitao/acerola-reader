package br.acerola.comic.module.main.history.state
import br.acerola.comic.ui.R

import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.history.ReadingHistoryWithChapterDto

sealed interface HistoryAction {
    data class ClickManga(val manga: ComicDto) : HistoryAction
    data class ClickContinue(val manga: ComicDto, val history: ReadingHistoryWithChapterDto) : HistoryAction
}
