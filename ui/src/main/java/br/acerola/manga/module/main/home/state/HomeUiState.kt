package br.acerola.manga.module.main.home.state

import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.config.preference.MangaSortType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.history.ReadingHistoryDto

data class FilterSettings(
    val showHidden: Boolean = false,
    val bookmarkCategoryId: Long? = null,
    val metadataSource: String? = null,
)

data class HomeUiState(
    val isIndexing: Boolean = false,
    val layout: HomeLayoutType = HomeLayoutType.LIST,
    val mangas: List<Triple<MangaDto, ReadingHistoryDto?, Int>> = emptyList(),
    val sortType: MangaSortType = MangaSortType.TITLE,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val filter: FilterSettings = FilterSettings()
)
