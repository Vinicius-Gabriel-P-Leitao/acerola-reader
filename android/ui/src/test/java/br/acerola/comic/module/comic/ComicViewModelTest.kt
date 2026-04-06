package br.acerola.comic.module.comic

import android.content.Context
import app.cash.turbine.test
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.config.preference.ChapterPageSizeType
import br.acerola.comic.config.preference.ChapterPerPagePreference
import br.acerola.comic.config.preference.ChapterSortPreference
import br.acerola.comic.config.preference.ChapterSortPreferenceData
import br.acerola.comic.config.preference.ChapterSortType
import br.acerola.comic.config.preference.SortDirection
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.history.ObserveComicHistoryUseCase
import br.acerola.comic.usecase.history.TrackReadingProgressUseCase
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
class ComicViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val historyGateway = mockk<HistoryGateway>(relaxed = true)
    private val trackReadingProgressUseCase = mockk<TrackReadingProgressUseCase>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val mangadexRepo = mockk<ComicGateway<ComicMetadataDto>>(relaxed = true)
    private val directoryRepo = mockk<ComicGateway<ComicDirectoryDto>>(relaxed = true)
    private val directoryChapterRepo = mockk<ChapterGateway<ChapterArchivePageDto>>(relaxed = true)
    private val mangadexChapterRepo = mockk<ChapterGateway<ChapterRemoteInfoPageDto>>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)

    private lateinit var observeComicHistoryUseCase: ObserveComicHistoryUseCase
    private lateinit var mangadexObserve: ObserveLibraryUseCase<ComicMetadataDto>
    private lateinit var directoryObserve: ObserveLibraryUseCase<ComicDirectoryDto>
    private lateinit var directoryGetChapters: ObserveChaptersUseCase<ChapterArchivePageDto>
    private lateinit var mangadexGetChapters: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>
    
    private val localChaptersFlow = MutableStateFlow(ChapterArchivePageDto(emptyList(), 20, 0, 0))
    private val remoteChaptersFlow = MutableStateFlow(ChapterRemoteInfoPageDto(emptyList(), 20, 0, 0))

    private lateinit var viewModel: ComicViewModel

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
        every { directoryRepo.observeLibrary() } returns MutableStateFlow(listOf(mockk<ComicDirectoryDto>(relaxed = true) { every { id } returns 1L }))
        
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

        observeComicHistoryUseCase = ObserveComicHistoryUseCase(historyGateway)
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

    private fun createViewModel() = ComicViewModel(
        ObserveComicHistoryUseCase = observeComicHistoryUseCase,
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
    fun `deve ordenar capitulos decimais corretamente — 0_01 antes de 0_02, 0_10 apos 0_09`() = runTest {
        // Bug: "0.1".toDouble() == "0.10".toDouble() == 0.1
        // causava Ch.0.10 aparecer logo após Ch.0.01, antes de Ch.0.02
        val ch001 = ChapterFileDto(id = 1L, name = "Ch. 0.01", path = "", chapterSort = "0.1")
        val ch002 = ChapterFileDto(id = 2L, name = "Ch. 0.02", path = "", chapterSort = "0.2")
        val ch009 = ChapterFileDto(id = 9L, name = "Ch. 0.09", path = "", chapterSort = "0.9")
        val ch010 = ChapterFileDto(id = 10L, name = "Ch. 0.10", path = "", chapterSort = "0.10")
        val ch011 = ChapterFileDto(id = 11L, name = "Ch. 0.11", path = "", chapterSort = "0.11")

        localChaptersFlow.value = ChapterArchivePageDto(
            listOf(ch010, ch009, ch001, ch011, ch002), 20, 0, 5
        )
        viewModel.updateChapterSort(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))

        viewModel.chapters.test {
            var item = awaitItem()
            while (item == null || item.archive.items.isEmpty()) item = awaitItem()

            val ids = item.archive.items.map { it.id }
            assertThat(ids).isEqualTo(listOf(1L, 2L, 9L, 10L, 11L))
        }
    }

    @Test
    fun `deve ordenar capitulos inteiros corretamente — 1 2 9 10 11 100`() = runTest {
        // Bug: "10".toDouble()=10.0 e "100".toDouble()=100.0 parecem ok,
        // mas "10" < "11" < "100" ficaria errado com sort textual.
        // Verifica que o sort numérico inteiro funciona.
        val ch1   = ChapterFileDto(id = 1L,   name = "Ch. 1",   path = "", chapterSort = "1")
        val ch2   = ChapterFileDto(id = 2L,   name = "Ch. 2",   path = "", chapterSort = "2")
        val ch9   = ChapterFileDto(id = 9L,   name = "Ch. 9",   path = "", chapterSort = "9")
        val ch10  = ChapterFileDto(id = 10L,  name = "Ch. 10",  path = "", chapterSort = "10")
        val ch11  = ChapterFileDto(id = 11L,  name = "Ch. 11",  path = "", chapterSort = "11")
        val ch100 = ChapterFileDto(id = 100L, name = "Ch. 100", path = "", chapterSort = "100")

        localChaptersFlow.value = ChapterArchivePageDto(
            listOf(ch100, ch10, ch1, ch11, ch9, ch2), 20, 0, 6
        )
        viewModel.updateChapterSort(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))

        viewModel.chapters.test {
            var item = awaitItem()
            while (item == null || item.archive.items.isEmpty()) item = awaitItem()

            val ids = item.archive.items.map { it.id }
            assertThat(ids).isEqualTo(listOf(1L, 2L, 9L, 10L, 11L, 100L))
        }
    }

    @Test
    fun `deve ordenar capitulos mistos inteiros e decimais corretamente`() = runTest {
        val ch001 = ChapterFileDto(id = 1L,  name = "Ch. 0.01", path = "", chapterSort = "0.1")
        val ch010 = ChapterFileDto(id = 2L,  name = "Ch. 0.10", path = "", chapterSort = "0.10")
        val ch1   = ChapterFileDto(id = 3L,  name = "Ch. 1",    path = "", chapterSort = "1")
        val ch1_5 = ChapterFileDto(id = 4L,  name = "Ch. 1.5",  path = "", chapterSort = "1.5")
        val ch2   = ChapterFileDto(id = 5L,  name = "Ch. 2",    path = "", chapterSort = "2")
        val ch10  = ChapterFileDto(id = 6L,  name = "Ch. 10",   path = "", chapterSort = "10")

        localChaptersFlow.value = ChapterArchivePageDto(
            listOf(ch10, ch1_5, ch010, ch2, ch1, ch001), 20, 0, 6
        )
        viewModel.updateChapterSort(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))

        viewModel.chapters.test {
            var item = awaitItem()
            while (item == null || item.archive.items.isEmpty()) item = awaitItem()

            val ids = item.archive.items.map { it.id }
            assertThat(ids).isEqualTo(listOf(1L, 2L, 3L, 4L, 5L, 6L))
        }
    }

    @Test
    fun `deve ordenar capitulos decimais descendente`() = runTest {
        val ch001 = ChapterFileDto(id = 1L, name = "Ch. 0.01", path = "", chapterSort = "0.1")
        val ch002 = ChapterFileDto(id = 2L, name = "Ch. 0.02", path = "", chapterSort = "0.2")
        val ch010 = ChapterFileDto(id = 3L, name = "Ch. 0.10", path = "", chapterSort = "0.10")

        localChaptersFlow.value = ChapterArchivePageDto(
            listOf(ch001, ch010, ch002), 20, 0, 3
        )
        viewModel.updateChapterSort(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.DESCENDING))

        viewModel.chapters.test {
            var item = awaitItem()
            while (item == null || item.archive.items.isEmpty()) item = awaitItem()

            val ids = item.archive.items.map { it.id }
            assertThat(ids).isEqualTo(listOf(3L, 2L, 1L))
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
