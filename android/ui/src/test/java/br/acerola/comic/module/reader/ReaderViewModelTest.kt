package br.acerola.comic.module.reader

import android.content.Context
import app.cash.turbine.test
import arrow.core.Either
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.adapter.contract.gateway.ChapterReadGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.config.preference.ReadingModePreference
import br.acerola.comic.config.preference.types.ReadingMode
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.service.reader.ReaderProcessor
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.history.TrackReadingProgressUseCase
import br.acerola.comic.usecase.reader.ReaderUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val processor = mockk<ReaderProcessor>(relaxed = true)
    private val historyGateway = mockk<HistoryGateway>(relaxed = true)
    private val readGateway = mockk<ChapterReadGateway<ChapterPageDto>>(relaxed = true)
    private val statusGateway = mockk<ChapterSyncStatusGateway>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private lateinit var readerUseCase: ReaderUseCase
    private lateinit var trackReadingProgressUseCase: TrackReadingProgressUseCase
    private lateinit var observeChaptersUseCase: ObserveChaptersUseCase<ChapterPageDto>

    private lateinit var viewModel: ReaderViewModel

    private val chapter1 = ChapterFileDto(1L, "Cap 1", "/path/1", "1")
    private val chapter2 = ChapterFileDto(2L, "Cap 2", "/path/2", "2")

    @Before
    fun setup() {
        mockkObject(ReadingModePreference)
        every { ReadingModePreference.readingModeFlow(any()) } returns flowOf(ReadingMode.HORIZONTAL)

        readerUseCase = ReaderUseCase(processor)
        trackReadingProgressUseCase = TrackReadingProgressUseCase(historyGateway)
        observeChaptersUseCase = ObserveChaptersUseCase(readGateway = readGateway, syncStatusGateway = statusGateway)

        every { readGateway.observeChapters(any(), any(), any()) } returns
            MutableStateFlow(ChapterPageDto(listOf(chapter1, chapter2), emptyList(), 20, 0, 2))
        every { statusGateway.isIndexing } returns MutableStateFlow(false)

        viewModel =
            ReaderViewModel(
                readerUseCase = readerUseCase,
                context = context,
                trackReadingProgressUseCase = trackReadingProgressUseCase,
                observeChaptersUseCase = observeChaptersUseCase,
            )
    }

    @After
    fun tearDown() {
        unmockkObject(ReadingModePreference)
    }

    @Test
    fun `deve atualizar o estado ao abrir um capitulo`() =
        runTest {
            coEvery { processor.openChapter(any()) } returns Either.Right(Unit)
            coEvery { processor.pageCount() } returns 10

            viewModel.openChapter(1L, chapter1, 0)

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.currentChapter).isEqualTo(chapter1)
                assertThat(state.pageCount).isEqualTo(10)
                assertThat(state.isLoading).isFalse()
            }
        }

    @Test
    fun `deve marcar capitulo como lido ao atingir 70 por cento das paginas`() =
        runTest {
            coEvery { processor.pageCount() } returns 10
            coEvery { processor.openChapter(any()) } returns Either.Right(Unit)

            viewModel.openChapter(1L, chapter1, 0)

            // Simula visualização de 7 páginas (70% de 10)
            for (i in 0..6) {
                viewModel.onPageVisible(1L, "1", 1L, i)
            }

            viewModel.uiState.test {
                assertThat(awaitItem().isChapterRead).isTrue()
            }

            coVerify { historyGateway.markChapterAsRead(1L, "1", 1L) }
        }

    @Test
    fun `deve salvar progresso no historico ao mudar de pagina`() =
        runTest {
            coEvery { processor.pageCount() } returns 10
            coEvery { processor.openChapter(any()) } returns Either.Right(Unit)

            viewModel.openChapter(1L, chapter1, 0)
            viewModel.onCurrentPageChanged(1L, "1", 1L, 5)

            coVerify { historyGateway.upsertHistory(any()) }
        }

    @Test
    fun `deve identificar capitulos adjacentes corretamente`() =
        runTest {
            coEvery { processor.openChapter(any()) } returns Either.Right(Unit)

            viewModel.openChapter(1L, chapter1, 0)

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.nextChapterId).isEqualTo(2L)
                assertThat(state.previousChapterId).isNull()
            }
        }
}
