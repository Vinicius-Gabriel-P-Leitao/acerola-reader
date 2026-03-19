package br.acerola.manga.module.reader

import android.content.Context
import app.cash.turbine.test
import arrow.core.Either
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.__fixtures__.MangaFixtures
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.config.preference.ReadingModePreference
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.engine.port.ChapterPort
import br.acerola.manga.engine.port.HistoryManagementRepository
import br.acerola.manga.service.reader.ChapterReaderService
import br.acerola.manga.usecase.chapter.ObserveChaptersUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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

    private val repository = mockk<ChapterReaderService>(relaxed = true)
    private val historyRepository = mockk<HistoryManagementRepository>(relaxed = true)
    private val chapterRepository = mockk<ChapterPort<ChapterArchivePageDto>>(relaxed = true)
    private lateinit var observeChaptersUseCase: ObserveChaptersUseCase<ChapterArchivePageDto>
    private val context = mockk<Context>(relaxed = true)

    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setup() {
        mockkObject(ReadingModePreference)
        mockkObject(AcerolaLogger)
        
        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.i(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit

        every { ReadingModePreference.readingModeFlow(any()) } returns flowOf(ReadingMode.HORIZONTAL)
        coEvery { ReadingModePreference.saveReadingMode(any(), any()) } returns Unit

        every { chapterRepository.observeChapters(any()) } returns MutableStateFlow(MangaFixtures.createChapterArchivePageDto())
        every { chapterRepository.isIndexing } returns MutableStateFlow(false)
        every { chapterRepository.progress } returns MutableStateFlow(-1)
        
        observeChaptersUseCase = ObserveChaptersUseCase(chapterRepository)
        
        every { repository.openChapter(any()) } returns Either.Right(Unit)
        coEvery { repository.pageCount() } returns 0

        viewModel = ReaderViewModel(repository, context, historyRepository, observeChaptersUseCase)
    }

    @After
    fun tearDown() {
        unmockkObject(ReadingModePreference)
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve inicializar com estado padrão`() = runTest {
        viewModel.state.test {
            val state = awaitItem()
            assertThat(state.readingMode).isEqualTo(ReadingMode.HORIZONTAL)
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `deve abrir capítulo e carregar navegação`() = runTest {
        val mangaId = 1L
        val chapters = MangaFixtures.createChapterList(3)
        val currentChapter = chapters[1] // Cap 2

        every { chapterRepository.observeChapters(mangaId) } returns MutableStateFlow(
            MangaFixtures.createChapterArchivePageDto(items = chapters)
        )
        every { repository.openChapter(any()) } returns Either.Right(Unit)
        coEvery { repository.pageCount() } returns 10

        viewModel.openChapter(mangaId, currentChapter)

        viewModel.state.test {
            val state = awaitItem()
            assertThat(state.currentChapter).isEqualTo(currentChapter)
            assertThat(state.pageCount).isEqualTo(10)
            assertThat(state.previousChapterId).isEqualTo(chapters[0].id)
            assertThat(state.nextChapterId).isEqualTo(chapters[2].id)
        }
    }

    @Test
    fun `deve atualizar página atual e persistir histórico`() = runTest {
        val mangaId = 1L
        val chapter = MangaFixtures.createChapterFileDto(id = 10L)
        
        coEvery { historyRepository.upsertHistory(any()) } returns Unit

        viewModel.onCurrentPageChanged(mangaId, chapter.id, 5)

        viewModel.state.test {
            assertThat(awaitItem().currentPage).isEqualTo(5)
        }
        
        io.mockk.coVerify { historyRepository.upsertHistory(any()) }
    }

    @Test
    fun `deve alternar visibilidade da UI`() = runTest {
        viewModel.state.test {
            val initial = awaitItem()
            assertThat(initial.isUiVisible).isTrue()
            
            viewModel.toggleUiVisibility()
            assertThat(awaitItem().isUiVisible).isFalse()
            
            viewModel.toggleUiVisibility()
            assertThat(awaitItem().isUiVisible).isTrue()
        }
    }
}
