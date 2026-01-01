package br.acerola.manga.repository.adapter.local.chapter

import br.acerola.manga.dto.archive.ChapterPageDto
import br.acerola.manga.local.database.dao.archive.ChapterFileDao
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.repository.port.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileChapterOperation @Inject constructor(
    private val chapterDao: ChapterFileDao
) : LibraryRepository.ChapterOperations<ChapterPageDto> {
    /**
     * Retorna um fluxo reativo contendo todos os capítulos pertencentes a um mangá específico.
     *
     * Cada entidade [ChapterFile] é convertida para [ChapterFileDto] por meio do mapeador [toDto].
     *
     * @param mangaId Identificador único do mangá.
     * @return [StateFlow] com a lista de capítulos atualizada dinamicamente.
     */
    override fun loadChapterByManga(mangaId: Long): StateFlow<ChapterPageDto> {
        return chapterDao.getChaptersByFolder(folderId = mangaId).map { list ->
            ChapterPageDto(
                items = list.map { it.toDto() }, pageSize = list.size, page = 0, total = list.size
            )
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = ChapterPageDto(items = emptyList(), pageSize = 0, page = 0, total = 0)
        )
    }


    override suspend fun loadNextPage(
        folderId: Long, total: Int, page: Int, pageSize: Int
    ): ChapterPageDto {
        val offset = page * pageSize
        val items = chapterDao.getChaptersPaged(folderId, pageSize, offset).firstOrNull()?.map {
            it.toDto()
        } ?: emptyList()

        return ChapterPageDto(
            items = items, pageSize = pageSize, page = page, total = total
        )
    }
}