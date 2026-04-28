package br.acerola.comic.module.main.home

import android.content.Context
import androidx.work.WorkManager
import app.cash.turbine.test
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.config.preference.ComicSortPreference
import br.acerola.comic.config.preference.ComicSortType
import br.acerola.comic.config.preference.HomeFilterPreference
import br.acerola.comic.config.preference.HomeLayoutPreference
import br.acerola.comic.config.preference.HomeLayoutType
import br.acerola.comic.config.preference.HomeSortPreference
import br.acerola.comic.config.preference.SortDirection
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.module.main.home.state.FilterSettings
import br.acerola.comic.usecase.chapter.GetChapterCountUseCase
import br.acerola.comic.usecase.comic.DeleteComicUseCase
import br.acerola.comic.usecase.comic.HideComicUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.history.ObserveHistoryUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
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
class HomeViewModelTest {
    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val historyGateway = mockk<HistoryGateway>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val mangadexRepo = mockk<ComicGateway<ComicMetadataDto>>(relaxed = true)
    private val directoryRepo = mockk<ComicGateway<ComicDirectoryDto>>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)
    private val hideComicUseCase = mockk<HideComicUseCase>(relaxed = true)
    private val deleteComicUseCase = mockk<DeleteComicUseCase>(relaxed = true)
    private val getChapterCountUseCase = mockk<GetChapterCountUseCase>(relaxed = true)

    private lateinit var observeHistoryUseCase: ObserveHistoryUseCase
    private lateinit var mangadexObserve: ObserveLibraryUseCase<ComicMetadataDto>
    private lateinit var directoryObserve: ObserveLibraryUseCase<ComicDirectoryDto>

    private val directoryFlow = MutableStateFlow<List<ComicDirectoryDto>>(emptyList())
    private val metadataFlow = MutableStateFlow<List<ComicMetadataDto>>(emptyList())
    private val chapterCountFlow = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val categoryMapFlow = MutableStateFlow<Map<Long, br.acerola.comic.dto.metadata.category.CategoryDto>>(emptyMap())

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        mockkObject(HomeLayoutPreference)
        mockkObject(ComicSortPreference)
        mockkObject(HomeFilterPreference)

        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any()) } returns Unit

        every { HomeLayoutPreference.layoutFlow(any()) } returns flowOf(HomeLayoutType.LIST)
        every { ComicSortPreference.sortFlow(any()) } returns flowOf(HomeSortPreference(ComicSortType.TITLE, SortDirection.ASCENDING))
        every { HomeFilterPreference.showHiddenFlow(any()) } returns flowOf(false)

        coEvery { HomeLayoutPreference.saveLayout(any(), any()) } returns Unit
        coEvery { ComicSortPreference.saveSort(any(), any()) } returns Unit
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
        unmockkObject(ComicSortPreference)
        unmockkObject(HomeFilterPreference)
    }

    private fun createViewModel() =
        HomeViewModel(
            workManager = workManager,
            observeHistoryUseCase = observeHistoryUseCase,
            context = context,
            mangadexObserve = mangadexObserve,
            directoryObserve = directoryObserve,
            manageCategoriesUseCase = manageCategoriesUseCase,
            hideComicUseCase = hideComicUseCase,
            deleteComicUseCase = deleteComicUseCase,
            getChapterCountUseCase = getChapterCountUseCase,
        )

    @Test
    fun `deve filtrar mangas ocultos por padrao`() =
        runTest {
            val manga1 =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 1L
                    every { name } returns "Manga 1"
                    every { hidden } returns false
                }
            val manga2 =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 2L
                    every { name } returns "Manga 2"
                    every { hidden } returns true
                }

            directoryFlow.value = listOf(manga1, manga2)

            viewModel.mangas.test {
                var items = awaitItem()
                while (items == null) items = awaitItem()
                assertThat(items).hasSize(1)
                assertThat(items[0].first.directory.id).isEqualTo(1L)
            }
        }

    @Test
    fun `deve mostrar mangas ocultos quando o filtro esta ativo`() =
        runTest {
            val manga1 =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 1L
                    every { hidden } returns false
                }
            val manga2 =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 2L
                    every { hidden } returns true
                }

            directoryFlow.value = listOf(manga1, manga2)
            viewModel.updateFilterSettings(FilterSettings(showHidden = true))

            viewModel.mangas.test {
                var items = awaitItem()
                while (items == null || items.size == 1) items = awaitItem()
                assertThat(items).hasSize(2)
            }
        }

    @Test
    fun `deve ordenar mangas por titulo`() =
        runTest {
            val mangaA =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 1L
                    every { name } returns "B Manga"
                    every { hidden } returns false
                }
            val mangaB =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 2L
                    every { name } returns "A Manga"
                    every { hidden } returns false
                }

            directoryFlow.value = listOf(mangaA, mangaB)
            viewModel.updateSortSettings(HomeSortPreference(ComicSortType.TITLE, SortDirection.ASCENDING))

            viewModel.mangas.test {
                var items = awaitItem()
                while (items == null || (items.size == 2 && items[0].first.directory.name == "B Manga")) items = awaitItem()
                assertThat(items[0].first.directory.name).isEqualTo("A Manga")
                assertThat(items[1].first.directory.name).isEqualTo("B Manga")
            }
        }

    @Test
    fun `deve ordenar mangas por titulo descendente`() =
        runTest {
            val mangaA =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 1L
                    every { name } returns "A Manga"
                    every { hidden } returns false
                }
            val mangaB =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 2L
                    every { name } returns "B Manga"
                    every { hidden } returns false
                }

            directoryFlow.value = listOf(mangaA, mangaB)
            viewModel.updateSortSettings(HomeSortPreference(ComicSortType.TITLE, SortDirection.DESCENDING))

            viewModel.mangas.test {
                var items = awaitItem()
                while (items == null || (items.size == 2 && items[0].first.directory.name == "A Manga")) items = awaitItem()
                assertThat(items[0].first.directory.name).isEqualTo("B Manga")
                assertThat(items[1].first.directory.name).isEqualTo("A Manga")
            }
        }

    @Test
    fun `deve filtrar por categoria`() =
        runTest {
            val cat1 =
                br.acerola.comic.dto.metadata.category
                    .CategoryDto(id = 10L, name = "Cat 1", color = 0)

            val manga1 =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 1L
                    every { hidden } returns false
                }
            val manga2 =
                mockk<ComicDirectoryDto>(relaxed = true) {
                    every { id } returns 2L
                    every { hidden } returns false
                }

            directoryFlow.value = listOf(manga1, manga2)
            categoryMapFlow.value = mapOf(1L to cat1)

            viewModel.updateFilterSettings(FilterSettings(bookmarkCategoryId = 10L))

            viewModel.mangas.test {
                var items = awaitItem()
                while (items == null || items.size == 2) items = awaitItem()
                assertThat(items).hasSize(1)
                assertThat(items[0].first.directory.id).isEqualTo(1L)
            }
        }
}
