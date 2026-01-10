package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import arrow.core.Either
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.repository.port.MangaManagementRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaDirectoryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val directoryDao: MangaDirectoryDao,
    private val archiveDao: ChapterArchiveDao
) : MangaManagementRepository<MangaDirectoryDto> {
    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun rescanManga(mangaId: Long): Either<LibrarySyncError, Unit> {
        TODO("Not yet implemented")
    }

    /**
     * Retorna um fluxo reativo contendo todos os mangás e seus capítulos associados, os dados são inseridos no sync.
     *
     * Combina as emissões de [MangaDirectoryDao] e [ChapterArchiveDao], convertendo o resultado para
     * objetos [MangaDirectoryDto] via [toDto].
     *
     * O fluxo é *stateful* e inicializa de forma preguiçosa (`SharingStarted.Lazily`).
     *
     * @return [StateFlow] contendo a lista de [MangaDirectoryDto] atualizada em tempo real.
     */
    override fun loadMangas(): StateFlow<List<MangaDirectoryDto>> {
        return directoryDao.getAllMangaDirectory().map { folders ->
            coroutineScope {
                folders.map { folder ->
                    async(context = Dispatchers.IO) {
                        folder.toDto()
                    }
                }.awaitAll()
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }
}
