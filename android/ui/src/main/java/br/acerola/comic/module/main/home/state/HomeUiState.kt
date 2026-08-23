package br.acerola.comic.module.main.home.state
import br.acerola.comic.config.preference.types.ComicSortType
import br.acerola.comic.config.preference.types.HomeLayoutType
import br.acerola.comic.config.preference.types.SortDirection
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.history.ReadingHistoryDto

data class FilterSettings(
    val showHidden: Boolean = false,
    val bookmarkCategoryId: Long? = null,
    val metadataSource: String? = null,
)

data class HomeUiState(
    val isIndexing: Boolean = false,
    val layout: HomeLayoutType = HomeLayoutType.LIST,
    val comics: List<Triple<ComicDto, ReadingHistoryDto?, Int>>? = null,
    val sortType: ComicSortType = ComicSortType.TITLE,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val filter: FilterSettings = FilterSettings(),
)
