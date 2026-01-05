package br.acerola.manga.repository.adapter.local.chapter

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.repository.port.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexChapterRemoteInfoOperation @Inject constructor(
    private val chapterRemoteInfoDao: ChapterRemoteInfoDao,
    private val chapterDownloadSourceDao: ChapterDownloadSourceDao
) : LibraryRepository.ChapterOperations<ChapterRemoteInfoPageDto> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun loadChapterByManga(mangaId: Long): StateFlow<ChapterRemoteInfoPageDto> {
        return chapterRemoteInfoDao.getChaptersByMangaRemoteInfo(mangaId).flatMapLatest { chapters ->
            val chapterIds = chapters.map { it.id }

            flow {
                val sources: List<ChapterDownloadSource> = if (chapterIds.isNotEmpty()) {
                    chapterDownloadSourceDao.getChapterDownloadSourceByRemoteInfoId(chapterIds).first()
                } else {
                    emptyList()
                }

                emit(
                    value = ChapterRemoteInfoPageDto(
                        items = chapters.map { it.toDto(sources) },
                        pageSize = chapters.size,
                        page = 0,
                        total = chapters.size
                    )
                )
            }
        }.stateIn(
            started = SharingStarted.Lazily,
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            initialValue = ChapterRemoteInfoPageDto(items = emptyList(), pageSize = 0, total = 0, page = 0)
        )
    }

    override suspend fun loadPage(
        mangaId: Long, total: Int, page: Int, pageSize: Int
    ): ChapterRemoteInfoPageDto {
        val offset = page * pageSize

        val realTotal = if (total > 0) {
            total
        } else {
            chapterRemoteInfoDao.countChaptersByMangaRemoteInfo(mangaId)
        }

        val chapters = chapterRemoteInfoDao.getChaptersPaged(mangaId, pageSize, offset)

        val sources = if (chapters.isNotEmpty()) {
            chapterDownloadSourceDao.getChapterDownloadSourceByRemoteInfoId(
                chapterId = chapters.map { it.id }).first()
        } else {
            emptyList()
        }

        val items = chapters.map { it.toDto(sources) }

        return ChapterRemoteInfoPageDto(
            items = items, page = page, pageSize = pageSize, total = realTotal
        )
    }
}