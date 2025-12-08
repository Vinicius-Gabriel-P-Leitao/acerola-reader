package br.acerola.manga.domain.service.library.chapter

import br.acerola.manga.domain.data.dao.database.archive.ChapterFileDao
import br.acerola.manga.domain.mapper.toDto
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.ChapterPageDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FileChapterOperation(
    private val chapterDao: ChapterFileDao
) : LibraryPort.ChapterOperations<ChapterPageDto> {
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