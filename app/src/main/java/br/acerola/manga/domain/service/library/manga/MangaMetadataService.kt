package br.acerola.manga.domain.service.library.manga

import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.domain.service.mangadex.FetchMangaDataMangaDexService
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MangaMetadataService : LibraryPort.MangaOperations<MangaMetadataDto> {
    private val _mangas = MutableStateFlow<List<MangaMetadataDto>>(value = emptyList())
    val mangas: StateFlow<List<MangaMetadataDto>> = _mangas.asStateFlow()

    override fun loadMangas(): StateFlow<List<MangaMetadataDto>> {
        TODO("Not yet implemented")
    }

    override suspend fun rescanChaptersByManga(mangaId: Long) {
        TODO("Not yet implemented")
    }
}