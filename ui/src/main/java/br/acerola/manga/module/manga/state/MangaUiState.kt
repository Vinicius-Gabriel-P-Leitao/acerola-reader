package br.acerola.manga.module.manga.state

import androidx.annotation.StringRes
import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.config.preference.ChapterSortPreferenceData
import br.acerola.manga.config.preference.ChapterSortType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.dto.ChapterDto
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.ui.R

enum class MainTab(@param:StringRes val titleRes: Int) {
    CHAPTERS(R.string.title_chapter_tabs_chapters),
    SETTINGS(R.string.title_chapter_tabs_settings)
}

data class MangaUiState(
    val manga: MangaDto,
    val chapters: ChapterDto?,
    val selectedTab: MainTab,
    val history: ReadingHistoryDto?,
    val readChapters: Set<Long>,
    val totalChapters: Int,
    val currentPage: Int,
    val totalPages: Int,
    val selectedChapterPerPage: ChapterPageSizeType,
    val chapterSortSettings: ChapterSortPreferenceData = ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING),
    val allCategories: List<CategoryDto> = emptyList()
)

data class MangaConfigUiState(
    val directory: MangaDirectoryDto,
    val remoteInfo: MangaMetadataDto?,
    val selectedChapterPerPage: ChapterPageSizeType?
)
