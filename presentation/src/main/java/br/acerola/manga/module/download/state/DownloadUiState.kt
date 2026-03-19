package br.acerola.manga.module.download.state

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto

data class DownloadUiState(
    val manga: MangaRemoteInfoDto? = null,
    val chapters: List<ChapterRemoteInfoDto> = emptyList(),
    val allSeenChapters: Map<String, ChapterRemoteInfoDto> = emptyMap(),
    val totalChapters: Int = 0,
    val currentPage: Int = 0,
    val chaptersPerPage: Int = 100,
    val isLoadingChapters: Boolean = false,
    val selectedLanguage: String = "pt-br",
    val selectedChapterIds: Set<String> = emptySet(),
    val isDownloading: Boolean = false,
) {
    val totalPages: Int
        get() = if (chaptersPerPage > 0) ((totalChapters + chaptersPerPage - 1) / chaptersPerPage) else 1
}
