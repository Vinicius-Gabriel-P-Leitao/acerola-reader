package br.acerola.manga.repository.adapter.local.chapter

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.local.mapper.toPageDto
import br.acerola.manga.repository.port.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterArchiveRepository @Inject constructor(
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
            list.toPageDto()
        }.stateIn(
            started = SharingStarted.Lazily,
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            initialValue = ChapterArchivePageDto(items = emptyList(), pageSize = 0, total = 0, page = 0)
        )
    }

    override suspend fun loadChapterPage(mangaId: Long, total: Int, page: Int, pageSize: Int): ChapterArchivePageDto {
        val offset = page * pageSize

        val realTotal = if (total > 0) {
            total
        } else {
            chapterArchiveDao.countChaptersByMangaDirectory(folderId = mangaId)
        }

        val items = chapterArchiveDao.getChaptersPaged(
            pageSize = pageSize, folderId = mangaId, offset = offset
        )

        return items.toPageDto(pageSize = pageSize, total = realTotal, page = page)
    }

    override fun observeSpecificChapters(
        mangaId: Long,
        chapters: List<String>
    ): kotlinx.coroutines.flow.Flow<ChapterArchivePageDto> {
        return chapterArchiveDao.getChaptersByMangaAndSorts(folderId = mangaId, chapters = chapters)
            .map { list ->
                list.toPageDto()
            }
    }
}