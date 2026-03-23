package br.acerola.manga.module.history

import app.cash.turbine.test
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.history.ReadingHistoryWithChapterDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.module.main.history.HistoryViewModel
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.core.usecase.chapter.GetChapterCountUseCase
import br.acerola.manga.core.usecase.history.ObserveHistoryUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.core.usecase.metadata.ManageCategoriesUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var directoryRepo: MangaGateway<MangaDirectoryDto>
    private lateinit var mangadexRepo: MangaGateway<MangaMetadataDto>
    private lateinit var manageCategoriesUseCase: ManageCategoriesUseCase
    private lateinit var getChapterCountUseCase: GetChapterCountUseCase

    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        observeHistoryUseCase = mockk()
        directoryRepo = mockk()
        mangadexRepo = mockk()
        manageCategoriesUseCase = mockk()
        getChapterCountUseCase = mockk()

        every { directoryRepo.isIndexing } returns MutableStateFlow(false)
        every { directoryRepo.progress } returns MutableStateFlow(-1)
        every { mangadexRepo.isIndexing } returns MutableStateFlow(false)
        every { mangadexRepo.progress } returns MutableStateFlow(-1)
        every { manageCategoriesUseCase.getAllMangaCategories() } returns flowOf(emptyMap())
        every { getChapterCountUseCase() } returns flowOf(emptyMap())
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
            chapterTemplateFk = null,
        )

        val observeHistoryResult = MutableStateFlow(listOf(historyDto))
        every { observeHistoryUseCase() } returns observeHistoryResult
        every { directoryRepo.observeLibrary() } returns MutableStateFlow(listOf(directoryDto))
        every { mangadexRepo.observeLibrary() } returns MutableStateFlow(emptyList())

        viewModel = HistoryViewModel(
            observeHistoryUseCase,
            ObserveLibraryUseCase(mangadexRepo),
            ObserveLibraryUseCase(directoryRepo),
            manageCategoriesUseCase,
            getChapterCountUseCase
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
