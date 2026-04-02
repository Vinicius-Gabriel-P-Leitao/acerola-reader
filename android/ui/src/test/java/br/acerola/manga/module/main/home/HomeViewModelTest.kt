package br.acerola.manga.module.main.home

import android.content.Context
import androidx.work.WorkManager
import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.adapter.contract.gateway.HistoryGateway
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.core.usecase.chapter.GetChapterCountUseCase
import br.acerola.manga.core.usecase.history.ObserveHistoryUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.core.usecase.manga.HideMangaUseCase
import br.acerola.manga.core.usecase.manga.DeleteMangaUseCase
import br.acerola.manga.core.usecase.metadata.ManageCategoriesUseCase
import br.acerola.manga.config.preference.MangaSortType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.config.preference.HomeSortPreference
import br.acerola.manga.config.preference.HomeLayoutPreference
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.config.preference.MangaSortPreference
import br.acerola.manga.config.preference.HomeFilterPreference
import br.acerola.manga.module.main.home.state.FilterSettings
import br.acerola.manga.logging.AcerolaLogger
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val historyGateway = mockk<HistoryGateway>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val mangadexRepo = mockk<MangaGateway<MangaMetadataDto>>(relaxed = true)
    private val directoryRepo = mockk<MangaGateway<MangaDirectoryDto>>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)
    private val hideMangaUseCase = mockk<HideMangaUseCase>(relaxed = true)
    private val deleteMangaUseCase = mockk<DeleteMangaUseCase>(relaxed = true)
    private val getChapterCountUseCase = mockk<GetChapterCountUseCase>(relaxed = true)

    private lateinit var observeHistoryUseCase: ObserveHistoryUseCase
    private lateinit var mangadexObserve: ObserveLibraryUseCase<MangaMetadataDto>
    private lateinit var directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>
    
    private val directoryFlow = MutableStateFlow<List<MangaDirectoryDto>>(emptyList())
    private val metadataFlow = MutableStateFlow<List<MangaMetadataDto>>(emptyList())
    private val chapterCountFlow = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val categoryMapFlow = MutableStateFlow<Map<Long, br.acerola.manga.dto.metadata.category.CategoryDto>>(emptyMap())

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        mockkObject(HomeLayoutPreference)
        mockkObject(MangaSortPreference)
        mockkObject(HomeFilterPreference)

        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any()) } returns Unit

        every { HomeLayoutPreference.layoutFlow(any()) } returns flowOf(HomeLayoutType.LIST)
        every { MangaSortPreference.sortFlow(any()) } returns flowOf(HomeSortPreference(MangaSortType.TITLE, SortDirection.ASCENDING))
        every { HomeFilterPreference.showHiddenFlow(any()) } returns flowOf(false)
        
        coEvery { HomeLayoutPreference.saveLayout(any(), any()) } returns Unit
        coEvery { MangaSortPreference.saveSort(any(), any()) } returns Unit
        coEvery { HomeFilterPreference.saveShowHidden(any(), any()) } returns Unit

        every { historyGateway.getAllRecentHistory() } returns MutableStateFlow(emptyList())
        every { mangadexRepo.observeLibrary() } returns metadataFlow
        every { directoryRepo.observeLibrary() } returns directoryFlow
        every { getChapterCountUseCase() } returns chapterCountFlow
        every { manageCategoriesUseCase.getAllMangaCategories() } returns categoryMapFlow
        every { manageCategoriesUseCase.getAllCategories() } returns MutableStateFlow(emptyList())

        observeHistoryUseCase = ObserveHistoryUseCase(historyGateway)
        mangadexObserve = ObserveLibraryUseCase(mangaRepository = mangadexRepo)
        directoryObserve = ObserveLibraryUseCase(mangaRepository = directoryRepo)

        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
        unmockkObject(HomeLayoutPreference)
        unmockkObject(MangaSortPreference)
        unmockkObject(HomeFilterPreference)
    }

    private fun createViewModel() = HomeViewModel(
        workManager = workManager,
        observeHistoryUseCase = observeHistoryUseCase,
        context = context,
        mangadexObserve = mangadexObserve,
        directoryObserve = directoryObserve,
        manageCategoriesUseCase = manageCategoriesUseCase,
        hideMangaUseCase = hideMangaUseCase,
        deleteMangaUseCase = deleteMangaUseCase,
        getChapterCountUseCase = getChapterCountUseCase
    )

    @Test
    fun `deve filtrar mangas ocultos por padrao`() = runTest {
        val manga1 = mockk<MangaDirectoryDto>(relaxed = true) {
            every { id } returns 1L
            every { name } returns "Manga 1"
            every { hidden } returns false
        }
        val manga2 = mockk<MangaDirectoryDto>(relaxed = true) {
            every { id } returns 2L
            every { name } returns "Manga 2"
            every { hidden } returns true
        }
        
        directoryFlow.value = listOf(manga1, manga2)

        viewModel.mangas.test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].first.directory.id).isEqualTo(1L)
        }
    }

    @Test
    fun `deve mostrar mangas ocultos quando o filtro esta ativo`() = runTest {
        val manga1 = mockk<MangaDirectoryDto>(relaxed = true) {
            every { id } returns 1L
            every { hidden } returns false
        }
        val manga2 = mockk<MangaDirectoryDto>(relaxed = true) {
            every { id } returns 2L
            every { hidden } returns true
        }
        
        directoryFlow.value = listOf(manga1, manga2)
        viewModel.updateFilterSettings(FilterSettings(showHidden = true))

        viewModel.mangas.test {
            var items = awaitItem()
            if (items.size == 1) items = awaitItem()
            assertThat(items).hasSize(2)
        }
    }

    @Test
    fun `deve ordenar mangas por titulo`() = runTest {
        val mangaA = mockk<MangaDirectoryDto>(relaxed = true) {
            every { id } returns 1L
            every { name } returns "B Manga"
            every { hidden } returns false
        }
        val mangaB = mockk<MangaDirectoryDto>(relaxed = true) {
            every { id } returns 2L
            every { name } returns "A Manga"
            every { hidden } returns false
        }
        
        directoryFlow.value = listOf(mangaA, mangaB)
        viewModel.updateSortSettings(HomeSortPreference(MangaSortType.TITLE, SortDirection.ASCENDING))

        viewModel.mangas.test {
            var items = awaitItem()
            if (items.size == 2 && items[0].first.directory.name == "B Manga") items = awaitItem()
            assertThat(items[0].first.directory.name).isEqualTo("A Manga")
            assertThat(items[1].first.directory.name).isEqualTo("B Manga")
        }
    }

    @Test
    fun `deve ordenar mangas por titulo descendente`() = runTest {
        val mangaA = mockk<MangaDirectoryDto>(relaxed = true) {
            every { id } returns 1L
            every { name } returns "A Manga"
            every { hidden } returns false
        }
        val mangaB = mockk<MangaDirectoryDto>(relaxed = true) {
            every { id } returns 2L
            every { name } returns "B Manga"
            every { hidden } returns false
        }
        
        directoryFlow.value = listOf(mangaA, mangaB)
        viewModel.updateSortSettings(HomeSortPreference(MangaSortType.TITLE, SortDirection.DESCENDING))

        viewModel.mangas.test {
            var items = awaitItem()
            if (items.size == 2 && items[0].first.directory.name == "A Manga") items = awaitItem()
            assertThat(items[0].first.directory.name).isEqualTo("B Manga")
            assertThat(items[1].first.directory.name).isEqualTo("A Manga")
        }
    }

    @Test
    fun `deve filtrar por categoria`() = runTest {
        val cat1 = br.acerola.manga.dto.metadata.category.CategoryDto(id = 10L, name = "Cat 1", color = 0)
        
        val manga1 = mockk<MangaDirectoryDto>(relaxed = true) { every { id } returns 1L; every { hidden } returns false }
        val manga2 = mockk<MangaDirectoryDto>(relaxed = true) { every { id } returns 2L; every { hidden } returns false }
        
        directoryFlow.value = listOf(manga1, manga2)
        categoryMapFlow.value = mapOf(1L to cat1)
        
        viewModel.updateFilterSettings(FilterSettings(bookmarkCategoryId = 10L))

        viewModel.mangas.test {
            var items = awaitItem()
            if (items.size == 2) items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].first.directory.id).isEqualTo(1L)
        }
    }
}
