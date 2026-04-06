package br.acerola.comic.common.viewmodel.library.archive

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.config.permission.FileSystemAccessManager
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.comic.CoverFromChapterUseCase
import br.acerola.comic.usecase.comic.DeleteComicUseCase
import br.acerola.comic.usecase.comic.HideComicUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ComicDirectoryViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val manager = mockk<FileSystemAccessManager>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val coverFromChapterUseCase = mockk<CoverFromChapterUseCase>(relaxed = true)
    private val hideComicUseCase = mockk<HideComicUseCase>(relaxed = true)
    private val deleteComicUseCase = mockk<DeleteComicUseCase>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)
    
    private val chapterRepo = mockk<ChapterGateway<ChapterArchivePageDto>>(relaxed = true)
    private val mangaRepo = mockk<ComicGateway<ComicDirectoryDto>>(relaxed = true)
    
    private lateinit var observeChaptersUseCase: ObserveChaptersUseCase<ChapterArchivePageDto>
    private lateinit var observeLibraryUseCase: ObserveLibraryUseCase<ComicDirectoryDto>
    
    private lateinit var viewModel: ComicDirectoryViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        every { AcerolaLogger.d(any<String>(), any<String>(), any<LogSource>()) } returns Unit
        every { AcerolaLogger.audit(any<String>(), any<String>(), any<LogSource>(), any<Map<String, String>>()) } returns Unit

        every { mangaRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { mangaRepo.isIndexing } returns MutableStateFlow(false)
        every { mangaRepo.progress } returns MutableStateFlow(-1)
        
        every { chapterRepo.isIndexing } returns MutableStateFlow(false)
        every { chapterRepo.progress } returns MutableStateFlow(-1)

        observeChaptersUseCase = ObserveChaptersUseCase(chapterRepo)
        observeLibraryUseCase = ObserveLibraryUseCase(mangaRepository = mangaRepo)

        viewModel = createViewModel()
    }

    private fun createViewModel() = ComicDirectoryViewModel(
        workManager = workManager,
        manager = manager,
        coverFromChapterUseCase = coverFromChapterUseCase,
        observeLibraryUseCase = observeLibraryUseCase,
        observeChaptersUseCase = observeChaptersUseCase,
        hideComicUseCase = hideComicUseCase,
        deleteComicUseCase = deleteComicUseCase,
        manageCategoriesUseCase = manageCategoriesUseCase
    )

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve sincronizar biblioteca ao inicializar`() {
        verify { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `deve emitir lista de diretorios da biblioteca`() = runTest {
        val directories = listOf(mockk<ComicDirectoryDto>())
        every { mangaRepo.observeLibrary() } returns MutableStateFlow(directories)
        
        viewModel = createViewModel()

        viewModel.mangaDirectories.test {
            assertThat(awaitItem()).isEqualTo(directories)
        }
    }

    @Test
    fun `deve refletir progresso do WorkManager`() = runTest {
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.RUNNING
        every { workInfo.progress.getInt(any<String>(), any<Int>()) } returns 75
        
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

        viewModel.rescanMangas()

        viewModel.progress.test {
            assertThat(awaitItem()).isEqualTo(75)
        }
    }
}
