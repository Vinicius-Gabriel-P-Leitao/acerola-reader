package br.acerola.comic.module.main.home.state
import br.acerola.comic.config.preference.HomeLayoutType
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.history.ReadingHistoryDto

sealed interface HomeAction {
    data class UpdateLayout(
        val layout: HomeLayoutType,
    ) : HomeAction

    data class ClickManga(
        val comic: ComicDto,
    ) : HomeAction

    data class ClickContinue(
        val comic: ComicDto,
        val history: ReadingHistoryDto,
    ) : HomeAction
}
