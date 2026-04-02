package br.acerola.manga.module.manga

import android.content.Context
import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.gateway.HistoryGateway
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.core.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.core.usecase.history.ObserveMangaHistoryUseCase
import br.acerola.manga.core.usecase.history.TrackReadingProgressUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.core.usecase.metadata.ManageCategoriesUseCase
import br.acerola.manga.config.preference.ChapterSortType
import br.acerola.manga.config.preference.ChapterSortPreferenceData
import br.acerola.manga.config.preference.ChapterPerPagePreference
import br.acerola.manga.config.preference.ChapterSortPreference
import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.logging.AcerolaLogger
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.coEvery
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
class MangaViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val historyGateway = mockk<HistoryGateway>(relaxed = true)
    private val trackReadingProgressUseCase = mockk<TrackReadingProgressUseCase>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val mangadexRepo = mockk<MangaGateway<MangaMetadataDto>>(relaxed = true)
    private val directoryRepo = mockk<MangaGateway<MangaDirectoryDto>>(relaxed = true)
    private val directoryChapterRepo = mockk<ChapterGateway<ChapterArchivePageDto>>(relaxed = true)
    private val mangadexChapterRepo = mockk<ChapterGateway<ChapterRemoteInfoPageDto>>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)

    private lateinit var observeMangaHistoryUseCase: ObserveMangaHistoryUseCase
    private lateinit var mangadexObserve: ObserveLibraryUseCase<MangaMetadataDto>
    private lateinit var directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>
    private lateinit var directoryGetChapters: ObserveChaptersUseCase<ChapterArchivePageDto>
    private lateinit var mangadexGetChapters: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>
    
    private val localChaptersFlow = MutableStateFlow(ChapterArchivePageDto(emptyList(), 20, 0, 0))
    private val remoteChaptersFlow = MutableStateFlow(ChapterRemoteInfoPageDto(emptyList(), 20, 0, 0))

    private lateinit var viewModel: MangaViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        mockkObject(ChapterPerPagePreference)
        mockkObject(ChapterSortPreference)

        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit

        every { ChapterPerPagePreference.chapterPerPageFlow(any()) } returns flowOf(ChapterPageSizeType.SHORT)
        every { ChapterSortPreference.sortFlow(any()) } returns flowOf(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))
        
        coEvery { ChapterPerPagePreference.saveChapterPerPage(any(), any()) } returns Unit
        coEvery { ChapterSortPreference.saveSort(any(), any()) } returns Unit

        every { historyGateway.getHistoryByMangaId(any()) } returns MutableStateFlow(null)
        every { historyGateway.getReadChaptersByMangaId(any()) } returns MutableStateFlow(emptyList())
        every { mangadexRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { directoryRepo.observeLibrary() } returns MutableStateFlow(listOf(mockk<MangaDirectoryDto>(relaxed = true) { every { id } returns 1L }))
        
        every { directoryRepo.isIndexing } returns MutableStateFlow(false)
        every { directoryRepo.progress } returns MutableStateFlow(-1)
        every { mangadexRepo.isIndexing } returns MutableStateFlow(false)
        every { mangadexRepo.progress } returns MutableStateFlow(-1)
        
        every { directoryChapterRepo.isIndexing } returns MutableStateFlow(false)
        every { directoryChapterRepo.progress } returns MutableStateFlow(-1)
        every { mangadexChapterRepo.isIndexing } returns MutableStateFlow(false)
        every { mangadexChapterRepo.progress } returns MutableStateFlow(-1)

        every { directoryChapterRepo.observeChapters(any()) } returns localChaptersFlow
        every { mangadexChapterRepo.observeChapters(any()) } returns remoteChaptersFlow

        observeMangaHistoryUseCase = ObserveMangaHistoryUseCase(historyGateway)
        mangadexObserve = ObserveLibraryUseCase(mangaRepository = mangadexRepo)
        directoryObserve = ObserveLibraryUseCase(mangaRepository = directoryRepo)
        directoryGetChapters = ObserveChaptersUseCase(directoryChapterRepo)
        mangadexGetChapters = ObserveChaptersUseCase(mangadexChapterRepo)

        viewModel = createViewModel()
        viewModel.init(1L, null)
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
        unmockkObject(ChapterPerPagePreference)
        unmockkObject(ChapterSortPreference)
    }

    private fun createViewModel() = MangaViewModel(
        observeMangaHistoryUseCase = observeMangaHistoryUseCase,
        trackReadingProgressUseCase = trackReadingProgressUseCase,
        context = context,
        mangadexObserve = mangadexObserve,
        directoryObserve = directoryObserve,
        directoryGetChapters = directoryGetChapters,
        mangadexGetChapters = mangadexGetChapters,
        manageCategoriesUseCase = manageCategoriesUseCase
    )

    @Test
    fun `deve ordenar capítulos por numero ascendente`() = runTest {
        val cap1 = ChapterFileDto(id = 1L, name = "Cap 1", path = "", chapterSort = "1", lastModified = 1000L)
        val cap2 = ChapterFileDto(id = 2L, name = "Cap 2", path = "", chapterSort = "2", lastModified = 500L)
        
        localChaptersFlow.value = ChapterArchivePageDto(listOf(cap2, cap1), 20, 0, 2)
        viewModel.updateChapterSort(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))

        viewModel.chapters.test {
            var item = awaitItem()
            // Skip initial empty state if needed
            while (item == null || item.archive.items.isEmpty()) {
                item = awaitItem()
            }
            
            assertThat(item.archive.items[0].id).isEqualTo(1L)
            assertThat(item.archive.items[1].id).isEqualTo(2L)
        }
    }

    @Test
    fun `deve ordenar capítulos por ultima atualizacao descendente`() = runTest {
        val cap1 = ChapterFileDto(id = 1L, name = "Cap 1", path = "", chapterSort = "1", lastModified = 1000L)
        val cap2 = ChapterFileDto(id = 2L, name = "Cap 2", path = "", chapterSort = "2", lastModified = 2000L)
        
        localChaptersFlow.value = ChapterArchivePageDto(listOf(cap1, cap2), 20, 0, 2)
        viewModel.updateChapterSort(ChapterSortPreferenceData(ChapterSortType.LAST_UPDATE, SortDirection.DESCENDING))

        viewModel.chapters.test {
            var item = awaitItem()
            while (item == null || item.archive.items.isEmpty()) {
                item = awaitItem()
            }
            
            assertThat(item.archive.items[0].id).isEqualTo(2L)
            assertThat(item.archive.items[1].id).isEqualTo(1L)
        }
    }
}
