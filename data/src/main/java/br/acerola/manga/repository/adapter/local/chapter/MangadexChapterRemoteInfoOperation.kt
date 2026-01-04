package br.acerola.manga.repository.adapter.local.chapter

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.repository.port.LibraryRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexChapterRemoteInfoOperation @Inject constructor(
    private val chapterRemoteInfoDao: ChapterRemoteInfoDao,
) : LibraryRepository.ChapterOperations<ChapterRemoteInfoPageDto> {
    override fun loadChapterByManga(mangaId: Long): StateFlow<ChapterRemoteInfoPageDto> {
        TODO("Not yet implemented")
    }

    override suspend fun loadPage(
        folderId: Long, total: Int, page: Int, pageSize: Int
    ): ChapterRemoteInfoPageDto {
        TODO("Not yet implemented")
    }

}