package br.acerola.manga.domain.service.library.manga

import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import kotlinx.coroutines.flow.StateFlow

class MangaMetadataService : LibraryPort.MangaOperations  {
    override fun loadMangas(): StateFlow<List<MangaFolderDto>> {
        TODO("Not yet implemented")
    }

    override suspend fun rescanChaptersByManga(mangaId: Long) {
        TODO("Not yet implemented")
    }
}