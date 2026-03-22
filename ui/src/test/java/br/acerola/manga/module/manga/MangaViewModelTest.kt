package br.acerola.manga.module.manga

import android.content.Context
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.adapter.contract.ChapterPort
import br.acerola.manga.adapter.contract.HistoryPort
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.core.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.core.usecase.history.ObserveMangaHistoryUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.core.usecase.metadata.ManageCategoriesUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MangaViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val historyPort = mockk<HistoryPort>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val mangadexRepo = mockk<MangaPort<MangaRemoteInfoDto>>(relaxed = true)
    private val directoryRepo = mockk<MangaPort<MangaDirectoryDto>>(relaxed = true)
    private val directoryChapterRepo = mockk<ChapterPort<ChapterArchivePageDto>>(relaxed = true)
    private val mangadexChapterRepo = mockk<ChapterPort<ChapterRemoteInfoPageDto>>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)

    private lateinit var observeMangaHistoryUseCase: ObserveMangaHistoryUseCase
    private lateinit var mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>
    private lateinit var directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>
    private lateinit var directoryGetChapters: ObserveChaptersUseCase<ChapterArchivePageDto>
    private lateinit var mangadexGetChapters: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>
    
    private lateinit var viewModel: MangaViewModel

    @Before
    fun setup() {
        every { historyPort.getHistoryByMangaId(any()) } returns MutableStateFlow(null)
        every { historyPort.getReadChaptersByMangaId(any()) } returns MutableStateFlow(emptyList())
        every { mangadexRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { directoryRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        
        every { directoryRepo.isIndexing } returns MutableStateFlow(false)
        every { directoryRepo.progress } returns MutableStateFlow(-1)
        every { mangadexRepo.isIndexing } returns MutableStateFlow(false)
        every { mangadexRepo.progress } returns MutableStateFlow(-1)
        
        every { directoryChapterRepo.isIndexing } returns MutableStateFlow(false)
        every { directoryChapterRepo.progress } returns MutableStateFlow(-1)
        every { mangadexChapterRepo.isIndexing } returns MutableStateFlow(false)
        every { mangadexChapterRepo.progress } returns MutableStateFlow(-1)

        observeMangaHistoryUseCase = ObserveMangaHistoryUseCase(historyPort)
        mangadexObserve = ObserveLibraryUseCase(mangadexRepo)
        directoryObserve = ObserveLibraryUseCase(directoryRepo)
        directoryGetChapters = ObserveChaptersUseCase(directoryChapterRepo)
        mangadexGetChapters = ObserveChaptersUseCase(mangadexChapterRepo)

        viewModel = createViewModel()
    }

    private fun createViewModel() = MangaViewModel(
        observeMangaHistoryUseCase = observeMangaHistoryUseCase,
        context = context,
        mangadexObserve = mangadexObserve,
        directoryObserve = directoryObserve,
        directoryGetChapters = directoryGetChapters,
        mangadexGetChapters = mangadexGetChapters,
        manageCategoriesUseCase = manageCategoriesUseCase
    )

    @Test
    fun `deve inicializar corretamente`() {
        assert(viewModel.manga.value == null)
    }
}
