package br.acerola.manga.common.viewmodel.library.metadata

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.adapter.port.ChapterPort
import br.acerola.manga.usecase.chapter.ObserveChaptersUseCase
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
class ChapterRemoteInfoViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val repo = mockk<ChapterPort<ChapterRemoteInfoPageDto>>(relaxed = true)
    private lateinit var observeChaptersUseCase: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>
    private lateinit var viewModel: ChapterRemoteInfoViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        every { AcerolaLogger.d(any<String>(), any<String>(), any<LogSource>()) } returns Unit
        every { AcerolaLogger.audit(any<String>(), any<String>(), any<LogSource>(), any<Map<String, String>>()) } returns Unit

        every { repo.isIndexing } returns MutableStateFlow(false)
        every { repo.progress } returns MutableStateFlow(-1)
        
        observeChaptersUseCase = ObserveChaptersUseCase(repo)
        viewModel = ChapterRemoteInfoViewModel(workManager, observeChaptersUseCase)
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve enfileirar trabalho de sincronizacao mangadex ao solicitar`() = runTest {
        viewModel.syncChaptersByMangadex(1L)
        verify { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `deve enfileirar trabalho de sincronizacao comicinfo ao solicitar`() = runTest {
        viewModel.syncChaptersByComicInfo(1L)
        verify { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `deve refletir progresso do WorkManager`() = runTest {
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.RUNNING
        every { workInfo.progress.getInt(any<String>(), any<Int>()) } returns 30
        
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

        viewModel.syncChaptersByMangadex(1L)

        viewModel.progress.test {
            assertThat(awaitItem()).isEqualTo(30)
        }
    }
}
