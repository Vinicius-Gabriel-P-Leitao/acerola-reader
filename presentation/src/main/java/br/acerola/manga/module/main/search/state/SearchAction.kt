package br.acerola.manga.module.main.search.state

import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    object Search : SearchAction
    data class SelectManga(val manga: MangaRemoteInfoDto) : SearchAction
    data class SelectLanguage(val language: String) : SearchAction
    data class ToggleChapter(val chapterId: String) : SearchAction
    data class ChangePage(val page: Int) : SearchAction
    object SelectAll : SearchAction
    object DeselectAll : SearchAction
    object Download : SearchAction
    object DownloadAll : SearchAction
    object BackToSearch : SearchAction
}
