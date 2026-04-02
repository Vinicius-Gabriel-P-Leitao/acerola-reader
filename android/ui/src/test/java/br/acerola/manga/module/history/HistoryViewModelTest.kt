package br.acerola.manga.module.history

import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.adapter.contract.gateway.HistoryGateway
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.core.usecase.chapter.GetChapterCountUseCase
import br.acerola.manga.core.usecase.history.ObserveHistoryUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.core.usecase.metadata.ManageCategoriesUseCase
import br.acerola.manga.module.main.history.HistoryViewModel
import br.acerola.manga.logging.AcerolaLogger
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
    private val mangadexRepo = mockk<MangaGateway<MangaMetadataDto>>(relaxed = true)
    private val directoryRepo = mockk<MangaGateway<MangaDirectoryDto>>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)
    private val getChapterCountUseCase = mockk<GetChapterCountUseCase>(relaxed = true)

    private lateinit var observeHistoryUseCase: ObserveHistoryUseCase
    private lateinit var mangadexObserve: ObserveLibraryUseCase<MangaMetadataDto>
    private lateinit var directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>
    
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

        viewModel = HistoryViewModel(
            observeHistoryUseCase = observeHistoryUseCase,
            mangadexObserve = mangadexObserve,
            directoryObserve = directoryObserve,
            manageCategoriesUseCase = manageCategoriesUseCase,
            getChapterCountUseCase = getChapterCountUseCase
        )
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve inicializar com lista vazia`() = runTest {
        viewModel.historyItems.test {
            assertThat(awaitItem()).isEmpty()
        }
    }
}
