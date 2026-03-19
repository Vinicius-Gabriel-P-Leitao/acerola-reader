package br.acerola.manga.common.viewmodel.library.archive

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.engine.port.ChapterPort
import br.acerola.manga.engine.port.MangaPort
import br.acerola.manga.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
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
class MangaDirectoryViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val manager = mockk<FileSystemAccessManager>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    
    private val chapterRepo = mockk<ChapterPort<ChapterArchivePageDto>>(relaxed = true)
    private val mangaRepo = mockk<MangaPort<MangaDirectoryDto>>(relaxed = true)
    
    private lateinit var observeChaptersUseCase: ObserveChaptersUseCase<ChapterArchivePageDto>
    private lateinit var observeLibraryUseCase: ObserveLibraryUseCase<MangaDirectoryDto>
    
    private lateinit var viewModel: MangaDirectoryViewModel

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
        observeLibraryUseCase = ObserveLibraryUseCase(mangaRepo)

        viewModel = MangaDirectoryViewModel(manager, observeChaptersUseCase, observeLibraryUseCase, workManager)
    }

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
        val directories = listOf(mockk<MangaDirectoryDto>())
        every { mangaRepo.observeLibrary() } returns MutableStateFlow(directories)
        
        // Re-instanciar para pegar o novo flow
        viewModel = MangaDirectoryViewModel(manager, observeChaptersUseCase, observeLibraryUseCase, workManager)

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
