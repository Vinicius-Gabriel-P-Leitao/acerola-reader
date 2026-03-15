package br.acerola.manga.module.main.home.state

import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.history.ReadingHistoryDto

sealed interface HomeAction {
    data class UpdateLayout(val layout: HomeLayoutType) : HomeAction
    data class ClickManga(val manga: MangaDto) : HomeAction
    data class ClickContinue(val manga: MangaDto, val history: ReadingHistoryDto) : HomeAction
}