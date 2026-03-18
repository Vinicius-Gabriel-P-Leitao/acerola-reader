package br.acerola.manga.module.download.state

sealed interface DownloadAction {
    data class SelectLanguage(val language: String) : DownloadAction
    data class ToggleChapter(val chapterId: String) : DownloadAction
    data class ChangePage(val page: Int) : DownloadAction
    object SelectAll : DownloadAction
    object DeselectAll : DownloadAction
    object Download : DownloadAction
    object DownloadAll : DownloadAction
}
