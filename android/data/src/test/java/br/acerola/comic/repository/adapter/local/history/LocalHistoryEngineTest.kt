package br.acerola.comic.repository.adapter.local.history

import br.acerola.comic.adapter.history.LocalHistoryEngine
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.local.dao.history.ReadingHistoryDao
import br.acerola.comic.local.entity.history.ReadingHistory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LocalHistoryEngineTest {
    private lateinit var readingHistoryDao: ReadingHistoryDao
    private lateinit var repository: LocalHistoryEngine

    @Before
    fun setUp() {
        readingHistoryDao = mockk()
        repository = LocalHistoryEngine(readingHistoryDao)
    }

    @Test
    fun `Deve retornar historico por manga id`() =
        runTest {
            val mangaId = 1L
            val historyEntity = ReadingHistory(mangaId, 10L, 5, false, 123456L)
            every { readingHistoryDao.observeHistoryByDirectoryId(mangaId) } returns flowOf(historyEntity)

            val result = repository.getHistoryByMangaId(mangaId).first()

            assertEquals(mangaId, result?.mangaDirectoryId)
            assertEquals(10L, result?.chapterArchiveId)
        }

    @Test
    fun `Deve salvar historico`() =
        runTest {
            val dto = ReadingHistoryDto(1L, 10L, 5, false, 123456L)
            coEvery { readingHistoryDao.upsertHistory(any()) } returns Unit

            repository.upsertHistory(dto)

            coVerify { readingHistoryDao.upsertHistory(match { it.mangaDirectoryId == 1L && it.chapterArchiveId == 10L }) }
        }

    @Test
    fun `Deve marcar capitulo como lido`() =
        runTest {
            val mangaId = 1L
            val chapterId = 10L
            coEvery { readingHistoryDao.upsertChapterRead(any()) } returns Unit

            repository.markChapterAsRead(mangaId, chapterId)

            coVerify { readingHistoryDao.upsertChapterRead(match { it.mangaDirectoryId == mangaId && it.chapterArchiveId == chapterId }) }
        }

    @Test
    fun `Deve desmarcar capitulo como lido`() =
        runTest {
            val chapterId = 10L
            coEvery { readingHistoryDao.deleteChapterRead(chapterId) } returns Unit

            repository.unmarkChapterAsRead(chapterId)

            coVerify { readingHistoryDao.deleteChapterRead(chapterId) }
        }
}
