package br.acerola.comic.module.comic.state

import androidx.annotation.StringRes
import br.acerola.comic.config.preference.ChapterPageSizeType
import br.acerola.comic.config.preference.ChapterSortPreferenceData
import br.acerola.comic.config.preference.ChapterSortType
import br.acerola.comic.config.preference.SortDirection
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.ui.R
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet

enum class MainTab(@param:StringRes val titleRes: Int) {
    CHAPTERS(R.string.title_chapter_tabs_chapters),
    SETTINGS(R.string.title_chapter_tabs_settings)
}

data class ComicUiState(
    val manga: ComicDto,
    val chapters: ChapterDto?,
    val selectedTab: MainTab,
    val history: ReadingHistoryDto?,
    val readChapters: PersistentSet<Long>,
    val totalChapters: Int,
    val currentPage: Int,
    val totalPages: Int,
    val selectedChapterPerPage: ChapterPageSizeType,
    val chapterSortSettings: ChapterSortPreferenceData = ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING),
    val allCategories: PersistentList<CategoryDto> = kotlinx.collections.immutable.persistentListOf()
)

data class ComicConfigUiState(
    val directory: ComicDirectoryDto,
    val remoteInfo: ComicMetadataDto?,
    val selectedChapterPerPage: ChapterPageSizeType?
)
