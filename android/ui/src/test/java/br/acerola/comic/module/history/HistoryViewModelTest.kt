package br.acerola.comic.module.history

import app.cash.turbine.test
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.module.main.history.HistoryViewModel
import br.acerola.comic.usecase.chapter.GetChapterCountUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.history.ObserveHistoryUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val historyGateway = mockk<HistoryGateway>(relaxed = true)
    private val mangadexRepo = mockk<ComicGateway<ComicMetadataDto>>(relaxed = true)
    private val directoryRepo = mockk<ComicGateway<ComicDirectoryDto>>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)
    private val getChapterCountUseCase = mockk<GetChapterCountUseCase>(relaxed = true)

    private lateinit var observeHistoryUseCase: ObserveHistoryUseCase
    private lateinit var mangadexObserve: ObserveLibraryUseCase<ComicMetadataDto>
    private lateinit var directoryObserve: ObserveLibraryUseCase<ComicDirectoryDto>

    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        every { AcerolaLogger.d(any(), any(), any()) } returns Unit

        every { historyGateway.getAllRecentHistory() } returns MutableStateFlow(emptyList())
        every { mangadexRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { directoryRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { manageCategoriesUseCase.getAllMangaCategories() } returns MutableStateFlow(emptyMap())
        every { getChapterCountUseCase() } returns MutableStateFlow(emptyMap())

        observeHistoryUseCase = ObserveHistoryUseCase(historyGateway)
        mangadexObserve = ObserveLibraryUseCase(mangaRepository = mangadexRepo)
        directoryObserve = ObserveLibraryUseCase(mangaRepository = directoryRepo)

        viewModel =
            HistoryViewModel(
                observeHistoryUseCase = observeHistoryUseCase,
                mangadexObserve = mangadexObserve,
                directoryObserve = directoryObserve,
                manageCategoriesUseCase = manageCategoriesUseCase,
                getChapterCountUseCase = getChapterCountUseCase,
            )
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve inicializar com lista vazia`() =
        runTest {
            viewModel.historyItems.test {
                assertThat(awaitItem()).isEmpty()
            }
        }
}
