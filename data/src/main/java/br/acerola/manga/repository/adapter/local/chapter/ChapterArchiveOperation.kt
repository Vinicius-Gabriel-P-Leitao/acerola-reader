package br.acerola.manga.repository.adapter.local.chapter

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
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

// FIXME: Renomear ChapterArchiveOperation para ChapterOperation (sem Archive)
@Singleton
class ChapterArchiveOperation @Inject constructor(
    private val chapterArchiveDao: ChapterArchiveDao,
) : LibraryRepository.ChapterOperations<ChapterArchivePageDto> {
    /**
     * Retorna um fluxo reativo contendo todos os capítulos pertencentes a um mangá específico, os capitulos do
     * arquivo são retornados de forma páginada.
     *
     * Cada entidade [ChapterArchive] é convertida para [ChapterArchivePageDto] por meio do mapeador [toDto].
     *
     * @param mangaId Identificador único do mangá.
     * @return [StateFlow] com a lista de capítulos atualizada dinamicamente.
     */
    override fun loadChapterByManga(mangaId: Long): StateFlow<ChapterArchivePageDto> {
        return chapterArchiveDao.getChaptersByMangaDirectory(folderId = mangaId).map { list: List<ChapterArchive> ->
            ChapterArchivePageDto(
                items = list.map { it.toDto() }, pageSize = list.size, page = 0, total = list.size
            )
        }.stateIn(
            started = SharingStarted.Lazily,
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            initialValue = ChapterArchivePageDto(items = emptyList(), pageSize = 0, page = 0, total = 0)
        )
    }

    override suspend fun loadPage(
        folderId: Long, total: Int, page: Int, pageSize: Int
    ): ChapterArchivePageDto {
        val offset = page * pageSize
        val items = chapterArchiveDao.getChaptersPaged(folderId, pageSize, offset).firstOrNull()?.map {
            it.toDto()
        } ?: emptyList()

        return ChapterArchivePageDto(
            items = items, pageSize = pageSize, page = page, total = total
        )
    }

}