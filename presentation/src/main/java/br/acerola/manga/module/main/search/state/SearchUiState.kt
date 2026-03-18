package br.acerola.manga.module.main.search.state

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searchResults: List<MangaRemoteInfoDto> = emptyList(),
    val selectedManga: MangaRemoteInfoDto? = null,
    val chapters: List<ChapterRemoteInfoDto> = emptyList(),
    val totalChapters: Int = 0,
    val currentPage: Int = 0,
    val chaptersPerPage: Int = 100,
    val isLoadingChapters: Boolean = false,
    val selectedLanguage: String = "pt-br",
    val selectedChapterIds: Set<String> = emptySet(),
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
) {
    val totalPages: Int get() = if (chaptersPerPage > 0) ((totalChapters + chaptersPerPage - 1) / chaptersPerPage) else 1
}
