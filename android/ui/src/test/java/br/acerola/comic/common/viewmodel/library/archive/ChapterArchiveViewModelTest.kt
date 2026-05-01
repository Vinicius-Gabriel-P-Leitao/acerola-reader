package br.acerola.comic.common.viewmodel.library.archive

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterArchiveViewModelTest {
    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val observeChaptersUseCase = mockk<ObserveChaptersUseCase<ChapterPageDto>>(relaxed = true)
    private lateinit var viewModel: ChapterArchiveViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        every { AcerolaLogger.d(any<String>(), any<String>(), any<LogSource>()) } returns Unit
        every { AcerolaLogger.audit(any<String>(), any<String>(), any<LogSource>(), any<Map<String, String>>()) } returns Unit

        viewModel = ChapterArchiveViewModel(workManager, mockk(relaxed = true), observeChaptersUseCase)
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve enfileirar trabalho de sincronizacao ao solicitar`() =
        runTest {
            viewModel.syncChaptersByMangaDirectory(1L)

            verify { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) }
        }

    @Test
    fun `deve refletir progresso do WorkManager`() =
        runTest {
            val workerId = UUID.randomUUID()
            val workInfo = mockk<WorkInfo>()
            every { workInfo.state } returns WorkInfo.State.RUNNING
            every { workInfo.progress.getInt("progress", -1) } returns 50

            // Mocking observeWorkStatus internally is hard,
            // but we can mock the flow returned by workManager
            every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

            // Precisamos disparar o observeWorkStatus através do enqueueSync
            viewModel.syncChaptersByMangaDirectory(1L)

            viewModel.progress.test {
                assertThat(awaitItem()).isEqualTo(50)
            }
            viewModel.isIndexing.test {
                assertThat(awaitItem()).isTrue()
            }
        }
}
