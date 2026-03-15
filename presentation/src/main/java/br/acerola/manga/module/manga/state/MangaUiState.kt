package br.acerola.manga.module.manga.state

import androidx.annotation.StringRes
import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.dto.ChapterDto
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.presentation.R

enum class MainTab(@param:StringRes val titleRes: Int) {
    CHAPTERS(R.string.title_chapter_tabs_chapters),
    SETTINGS(R.string.title_chapter_tabs_settings)
}

data class MangaUiState(
    val manga: MangaDto,
    val chapters: ChapterDto?,
    val selectedTab: MainTab,
    val isIndexing: Boolean,
    val indexingProgress: Float?,
    val history: ReadingHistoryDto?,
    val readChapters: Set<Long>,
    val totalChapters: Int,
    val currentPage: Int,
    val totalPages: Int,
    val selectedChapterPerPage: ChapterPageSizeType
)

data class MangaConfigUiState(
    val directory: MangaDirectoryDto,
    val remoteInfo: MangaRemoteInfoDto?,
    val selectedChapterPerPage: ChapterPageSizeType?
)
