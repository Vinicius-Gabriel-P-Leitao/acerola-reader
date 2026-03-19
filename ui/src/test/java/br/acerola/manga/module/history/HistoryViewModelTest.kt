package br.acerola.manga.module.history

import app.cash.turbine.test
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.main.history.HistoryViewModel
import br.acerola.manga.engine.port.MangaPort
import br.acerola.manga.usecase.history.ObserveHistoryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var observeHistoryUseCase: ObserveHistoryUseCase
    private lateinit var directoryRepo: MangaPort<MangaDirectoryDto>
    private lateinit var mangadexRepo: MangaPort<MangaRemoteInfoDto>

    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        observeHistoryUseCase = mockk()
        directoryRepo = mockk()
        mangadexRepo = mockk()

        every { directoryRepo.isIndexing } returns MutableStateFlow(false)
        every { directoryRepo.progress } returns MutableStateFlow(-1)
        every { mangadexRepo.isIndexing } returns MutableStateFlow(false)
        every { mangadexRepo.progress } returns MutableStateFlow(-1)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Deve carregar lista de historico com sucesso`() = runTest {
        // Arrange
        val mangaId = 1L
        val historyDto = ReadingHistoryWithChapterDto(
            mangaDirectoryId = mangaId,
            chapterArchiveId = 10L,
            lastPage = 5,
            isCompleted = false,
            updatedAt = 123456L,
            chapterName = "Cap 1"
        )
        val directoryDto = MangaDirectoryDto(
            id = mangaId,
            name = "Test Manga",
            path = "path",
            coverUri = null,
            bannerUri = null,
            lastModified = 0L,
            chapterTemplate = "",
            hasComicInfo = false
        )

        every { observeHistoryUseCase() } returns MutableStateFlow(listOf(historyDto))
        every { directoryRepo.observeLibrary() } returns MutableStateFlow(listOf(directoryDto))
        every { mangadexRepo.observeLibrary() } returns MutableStateFlow(emptyList())

        viewModel = HistoryViewModel(
            observeHistoryUseCase,
            ObserveLibraryUseCase(mangadexRepo),
            ObserveLibraryUseCase(directoryRepo)
        )

        // Act & Assert
        viewModel.historyItems.test {
            val initial = awaitItem()
            assertEquals(0, initial.size)

            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Test Manga", result[0].manga.directory.name)
            assertEquals("Cap 1", result[0].history.chapterName)
        }
    }
}
