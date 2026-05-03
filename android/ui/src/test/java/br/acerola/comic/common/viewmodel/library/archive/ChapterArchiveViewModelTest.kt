package br.acerola.comic.common.viewmodel.library.archive

import app.cash.turbine.test
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterArchiveViewModelTest {
    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val scheduler = mockk<br.acerola.comic.sync.LibrarySyncScheduler>(relaxed = true)
    private val statusRepository = mockk<br.acerola.comic.sync.LibrarySyncStatusRepository>(relaxed = true)
    private val observeChaptersUseCase = mockk<ObserveChaptersUseCase<ChapterPageDto>>(relaxed = true)
    private lateinit var viewModel: ChapterArchiveViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        every { AcerolaLogger.d(any<String>(), any<String>(), any<LogSource>()) } returns Unit
        every { AcerolaLogger.audit(any<String>(), any<String>(), any<LogSource>(), any<Map<String, String>>()) } returns Unit

        every { statusRepository.isIndexing } returns MutableStateFlow(false)
        every { statusRepository.progress } returns MutableStateFlow(-1)

        viewModel = ChapterArchiveViewModel(scheduler, statusRepository, mockk(relaxed = true), observeChaptersUseCase)
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve enfileirar trabalho de sincronizacao ao solicitar`() =
        runTest {
            viewModel.syncChaptersByMangaDirectory(1L)

            verify { scheduler.enqueueSpecific(1L, any()) }
        }

    @Test
    fun `deve refletir progresso do repositório`() =
        runTest {
            val progressFlow = MutableStateFlow(50)
            val isIndexingFlow = MutableStateFlow(true)
            every { statusRepository.progress } returns progressFlow
            every { statusRepository.isIndexing } returns isIndexingFlow

            // Reinicializar viewModel para capturar os novos flows
            viewModel = ChapterArchiveViewModel(scheduler, statusRepository, mockk(relaxed = true), observeChaptersUseCase)

            viewModel.progress.test {
                assertThat(awaitItem()).isEqualTo(50)
            }
            viewModel.isIndexing.test {
                assertThat(awaitItem()).isTrue()
            }
        }
}
