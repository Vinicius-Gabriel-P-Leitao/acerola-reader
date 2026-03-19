package br.acerola.manga.module.manga

import android.content.Context
import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.__fixtures__.MangaFixtures
import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.config.preference.ChapterPerPagePreference
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.repository.port.HistoryManagementRepository
import br.acerola.manga.repository.port.MangaManagementRepository
import br.acerola.manga.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import com.google.common.truth.Truth.assertThat
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
class MangaViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    
    private val mangadexRepo = mockk<MangaManagementRepository<MangaRemoteInfoDto>>(relaxed = true)
    private val directoryRepo = mockk<MangaManagementRepository<MangaDirectoryDto>>(relaxed = true)
    private val directoryChapterRepo = mockk<ChapterManagementRepository<ChapterArchivePageDto>>(relaxed = true)
    private val mangadexChapterRepo = mockk<ChapterManagementRepository<ChapterRemoteInfoPageDto>>(relaxed = true)
    
    private lateinit var mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>
    private lateinit var directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>
    private lateinit var directoryGetChapters: ObserveChaptersUseCase<ChapterArchivePageDto>
    private lateinit var mangadexGetChapters: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>
    
    private val historyRepository = mockk<HistoryManagementRepository>(relaxed = true)

    private lateinit var viewModel: MangaViewModel

    @Before
    fun setup() {
        mockkObject(ChapterPerPagePreference)
        mockkObject(AcerolaLogger)
        
        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.i(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit
        
        every { ChapterPerPagePreference.chapterPerPageFlow(any()) } returns flowOf(ChapterPageSizeType.SHORT)
        
        val indexingFlow = MutableStateFlow(false)
        val progressFlow = MutableStateFlow(-1)
        
        every { mangadexRepo.isIndexing } returns indexingFlow
        every { mangadexRepo.progress } returns progressFlow
        every { mangadexRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        
        every { directoryRepo.isIndexing } returns indexingFlow
        every { directoryRepo.progress } returns progressFlow
        every { directoryRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        
        every { directoryChapterRepo.isIndexing } returns indexingFlow
        every { directoryChapterRepo.progress } returns progressFlow
        every { directoryChapterRepo.observeChapters(any()) } returns MutableStateFlow(MangaFixtures.createChapterArchivePageDto(emptyList()))
        
        every { mangadexChapterRepo.isIndexing } returns indexingFlow
        every { mangadexChapterRepo.progress } returns progressFlow
        every { mangadexChapterRepo.observeChapters(any()) } returns MutableStateFlow(ChapterRemoteInfoPageDto(emptyList(), 0, 0, 0))

        mangadexObserve = ObserveLibraryUseCase(mangadexRepo)
        directoryObserve = ObserveLibraryUseCase(directoryRepo)
        directoryGetChapters = ObserveChaptersUseCase(directoryChapterRepo)
        mangadexGetChapters = ObserveChaptersUseCase(mangadexChapterRepo)

        every { historyRepository.getHistoryByMangaId(any()) } returns flowOf(null)
        every { historyRepository.getReadChaptersByMangaId(any()) } returns flowOf(emptyList())

        viewModel = MangaViewModel(
            context,
            mangadexObserve,
            directoryObserve,
            directoryGetChapters,
            mangadexGetChapters,
            historyRepository
        )
    }

    @After
    fun tearDown() {
        unmockkObject(ChapterPerPagePreference)
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve inicializar com valores padrão`() = runTest {
        viewModel.selectedChapterPerPage.test {
            assertThat(awaitItem()).isEqualTo(ChapterPageSizeType.SHORT)
        }
    }
}
