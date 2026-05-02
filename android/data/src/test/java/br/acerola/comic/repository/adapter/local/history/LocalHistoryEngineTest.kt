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
    fun `Deve retornar historico por comic id`() =
        runTest {
            val comicId = 1L
            val historyEntity = ReadingHistory(comicId, "10", 10L, 5, false, 123456L)
            every { readingHistoryDao.observeHistoryByDirectoryId(comicId) } returns flowOf(historyEntity)

            val result = repository.getHistoryByMangaId(comicId).first()

            assertEquals(comicId, result?.comicDirectoryId)
            assertEquals("10", result?.chapterSort)
            assertEquals(10L, result?.chapterArchiveId)
        }

    @Test
    fun `Deve salvar historico`() =
        runTest {
            val dto = ReadingHistoryDto(1L, 10L, "10", 5, false, 123456L)
            coEvery { readingHistoryDao.upsertHistory(any()) } returns Unit

            repository.upsertHistory(dto)

            coVerify { readingHistoryDao.upsertHistory(match { it.comicDirectoryId == 1L && it.chapterSort == "10" }) }
        }

    @Test
    fun `Deve marcar capitulo como lido`() =
        runTest {
            val comicId = 1L
            val chapterSort = "10"
            val chapterId = 10L
            coEvery { readingHistoryDao.upsertChapterRead(any()) } returns Unit

            repository.markChapterAsRead(comicId, chapterSort, chapterId)

            coVerify { readingHistoryDao.upsertChapterRead(match { it.comicDirectoryId == comicId && it.chapterSort == chapterSort }) }
        }

    @Test
    fun `Deve desmarcar capitulo como lido`() =
        runTest {
            val comicId = 1L
            val chapterSort = "10"
            coEvery { readingHistoryDao.deleteChapterRead(comicId, chapterSort) } returns Unit

            repository.unmarkChapterAsRead(comicId, chapterSort)

            coVerify { readingHistoryDao.deleteChapterRead(comicId, chapterSort) }
        }
}
