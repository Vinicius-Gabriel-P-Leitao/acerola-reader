package br.acerola.manga.module.main.home.state

import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.history.ReadingHistoryDto

data class HomeUiState(
    val isIndexing: Boolean = false,
    val indexingProgress: Float? = null,
    val layout: HomeLayoutType = HomeLayoutType.LIST,
    val mangas: List<Triple<MangaDto, ReadingHistoryDto?, Int>> = emptyList()
)
