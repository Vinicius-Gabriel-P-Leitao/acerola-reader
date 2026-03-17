package br.acerola.manga.module.main.search.state

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searchResults: List<MangaRemoteInfoDto> = emptyList(),
    val selectedManga: MangaRemoteInfoDto? = null,
    val chapters: List<ChapterRemoteInfoDto> = emptyList(),
    val isLoadingChapters: Boolean = false,
    val selectedLanguage: String = "pt-br",
    val selectedChapterIds: Set<String> = emptySet(),
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
)
