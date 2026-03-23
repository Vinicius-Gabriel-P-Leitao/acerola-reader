package br.acerola.manga.module.download.state

import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto

import br.acerola.manga.module.main.search.state.DownloadProgress

data class DownloadUiState(
    val manga: MangaMetadataDto? = null,
    val chapters: List<ChapterMetadataDto> = emptyList(),
    val allSeenChapters: Map<String, ChapterMetadataDto> = emptyMap(),
    val totalChapters: Int = 0,
    val currentPage: Int = 0,
    val chaptersPerPage: Int = 100,
    val isLoadingChapters: Boolean = false,
    val selectedLanguage: String = "pt-br",
    val selectedChapterIds: Set<String> = emptySet(),
    val isDownloading: Boolean = false,
    val activeDownload: DownloadProgress? = null,
) {
    val totalPages: Int
        get() = if (chaptersPerPage > 0) ((totalChapters + chaptersPerPage - 1) / chaptersPerPage) else 1
}
