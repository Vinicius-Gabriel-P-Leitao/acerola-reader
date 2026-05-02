package br.acerola.comic.usecase.history

import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.dto.history.ReadingHistoryDto
import io.mockk.MockKAnnotations
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TrackReadingProgressUseCaseTest {
    @MockK
    lateinit var historyRepository: HistoryGateway

    private lateinit var useCase: TrackReadingProgressUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = TrackReadingProgressUseCase(historyRepository = historyRepository)
    }

    // markChapterAsRead — caminho feliz
    @Test
    fun `markChapterAsRead deve delegar ao repositório exatamente uma vez`() =
        runTest {
            coJustRun { historyRepository.markChapterAsRead(any(), any(), any()) }

            useCase.markChapterAsRead(comicId = 1L, chapterSort = "001", chapterId = 10L)

            coVerify(exactly = 1) { historyRepository.markChapterAsRead(1L, "001", 10L) }
        }

    // toggleReadStatus — isRead = true → desmarcar capítulo
    @Test
    fun `toggleReadStatus quando isRead=true deve chamar unmarkChapterAsRead`() =
        runTest {
            coJustRun { historyRepository.unmarkChapterAsRead(any(), any()) }

            useCase.toggleReadStatus(comicId = 1L, chapterSort = "001", isRead = true)

            coVerify(exactly = 1) { historyRepository.unmarkChapterAsRead(1L, "001") }
        }

    // toggleReadStatus — isRead = false → marcar capítulo
    @Test
    fun `toggleReadStatus quando isRead=false deve chamar markChapterAsRead`() =
        runTest {
            coJustRun { historyRepository.markChapterAsRead(any(), any(), any()) }

            useCase.toggleReadStatus(comicId = 1L, chapterSort = "001", isRead = false, chapterId = 10L)

            coVerify(exactly = 1) { historyRepository.markChapterAsRead(1L, "001", 10L) }
        }

    // saveProgress — caminho feliz
    @Test
    fun `saveProgress deve delegar upsertHistory ao repositório exatamente uma vez`() =
        runTest {
            val historyDto = mockk<ReadingHistoryDto>()
            coJustRun { historyRepository.upsertHistory(any()) }

            useCase.saveProgress(historyDto)

            coVerify(exactly = 1) { historyRepository.upsertHistory(historyDto) }
        }
}
