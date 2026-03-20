package br.acerola.manga.module.main.home

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.__fixtures__.MangaFixtures
import br.acerola.manga.config.preference.HomeLayoutPreference
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.adapter.contract.HistoryPort
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
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

    private val workManager = mockk<WorkManager>()
    private val historyRepository = mockk<HistoryPort>()
    
    private val mangadexRepo = mockk<MangaPort<MangaRemoteInfoDto>>(relaxed = true)
    private val directoryRepo = mockk<MangaPort<MangaDirectoryDto>>(relaxed = true)
    
    private lateinit var mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>
    private lateinit var directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>
    
    private val context = mockk<Context>(relaxed = true)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        mockkObject(HomeLayoutPreference)
        mockkObject(AcerolaLogger)
        
        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit
        
        every { HomeLayoutPreference.layoutFlow(any()) } returns flowOf(HomeLayoutType.LIST)
        coEvery { HomeLayoutPreference.saveLayout(any(), any()) } returns Unit
        
        val indexingFlow = MutableStateFlow(false)
        val progressFlow = MutableStateFlow(-1)
        
        every { workManager.getWorkInfosByTagFlow(any()) } returns flowOf(emptyList())
        every { historyRepository.getAllRecentHistory() } returns MutableStateFlow(emptyList())
        
        every { mangadexRepo.isIndexing } returns indexingFlow
        every { mangadexRepo.progress } returns progressFlow
        every { mangadexRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        
        every { directoryRepo.isIndexing } returns indexingFlow
        every { directoryRepo.progress } returns progressFlow
        every { directoryRepo.observeLibrary() } returns MutableStateFlow(emptyList())

        mangadexObserve = ObserveLibraryUseCase(mangadexRepo)
        directoryObserve = ObserveLibraryUseCase(directoryRepo)

        viewModel = HomeViewModel(
            workManager,
            historyRepository,
            context,
            mangadexObserve,
            directoryObserve
        )
    }

    @After
    fun tearDown() {
        unmockkObject(HomeLayoutPreference)
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve inicializar com layout padrão LIST`() = runTest {
        viewModel.selectedHomeLayout.test {
            assertThat(awaitItem()).isEqualTo(HomeLayoutType.LIST)
        }
    }

    @Test
    fun `deve atualizar layout quando solicitado`() = runTest {
        viewModel.updateHomeLayout(HomeLayoutType.GRID)
        
        viewModel.selectedHomeLayout.test {
            assertThat(awaitItem()).isEqualTo(HomeLayoutType.GRID)
        }
    }

    @Test
    fun `deve emitir lista de mangas quando repositorios atualizarem`() = runTest {
        val mangaDir = MangaFixtures.createMangaDirectoryDto(id = 1L, name = "Manga Test")
        val history = MangaFixtures.createReadingHistoryDto(mangaDirectoryId = 1L)

        every { directoryRepo.observeLibrary() } returns MutableStateFlow(listOf(mangaDir))
        every { mangadexRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { historyRepository.getAllRecentHistory() } returns MutableStateFlow(listOf(history))

        // Re-instanciar para pegar os novos flows
        viewModel = HomeViewModel(workManager, historyRepository, context, mangadexObserve, directoryObserve)

        viewModel.mangas.test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].first.directory.name).isEqualTo("Manga Test")
            assertThat(items[0].second?.mangaDirectoryId).isEqualTo(1L)
        }
    }

    @Test
    fun `deve refletir estado de indexação do WorkManager`() = runTest {
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.RUNNING
        every { workManager.getWorkInfosByTagFlow("library_sync") } returns flowOf(listOf(workInfo))

        viewModel = HomeViewModel(workManager, historyRepository, context, mangadexObserve, directoryObserve)

        viewModel.isIndexing.test {
            assertThat(awaitItem()).isTrue()
        }
    }
}
