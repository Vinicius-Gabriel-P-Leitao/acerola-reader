package br.acerola.comic.module.reader.state
import br.acerola.comic.config.preference.types.ReadingMode

sealed interface ReaderAction {
    data object NavigateBack : ReaderAction

    data object ToggleUi : ReaderAction

    data class UpdateReadingMode(
        val mode: ReadingMode,
    ) : ReaderAction

    data class ChangePage(
        val index: Int,
    ) : ReaderAction

    data object LoadNextChapter : ReaderAction

    data object LoadPreviousChapter : ReaderAction

    data class PageVisible(
        val index: Int,
    ) : ReaderAction

    data class CurrentPageChanged(
        val index: Int,
    ) : ReaderAction
}
