package br.acerola.manga.module.main.search.state

import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searchResults: List<MangaRemoteInfoDto> = emptyList(),
    val downloadQueue: List<DownloadProgress> = emptyList(),
)
