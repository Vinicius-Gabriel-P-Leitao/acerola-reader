package br.acerola.manga.domain.service.library.manga

import br.acerola.manga.domain.data.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.data.dao.database.metadata.MangaMetadataDao
import br.acerola.manga.domain.mapper.toDto
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MangaDexMangaOperation(
    private val mangaDao: MangaMetadataDao,
    private val folderDao: MangaFolderDao,
) : LibraryPort.MangaOperations<MangaMetadataDto> {
    private val _mangas = MutableStateFlow<List<MangaMetadataDto>>(value = emptyList())
    val mangas: StateFlow<List<MangaMetadataDto>> = _mangas.asStateFlow()

    override suspend fun rescanChaptersByManga(mangaId: Long) {

    }

    override fun loadMangas(): StateFlow<List<MangaMetadataDto>> {
        return mangaDao.getAllMangasWithRelations().map { relationsList ->
            relationsList.map { relation ->
                relation.toDto()
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }
}