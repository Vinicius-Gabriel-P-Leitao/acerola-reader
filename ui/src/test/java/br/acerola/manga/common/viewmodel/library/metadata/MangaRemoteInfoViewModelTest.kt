package br.acerola.manga.common.viewmodel.library.metadata

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.adapter.contract.MangaPort
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
class MangaRemoteInfoViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val repo = mockk<MangaPort<MangaRemoteInfoDto>>(relaxed = true)
    private lateinit var observeUseCase: ObserveLibraryUseCase<MangaRemoteInfoDto>
    private lateinit var viewModel: MangaRemoteInfoViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        every { AcerolaLogger.d(any<String>(), any<String>(), any<LogSource>()) } returns Unit
        every { AcerolaLogger.audit(any<String>(), any<String>(), any<LogSource>(), any<Map<String, String>>()) } returns Unit

        every { repo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { repo.isIndexing } returns MutableStateFlow(false)
        every { repo.progress } returns MutableStateFlow(-1)
        
        observeUseCase = ObserveLibraryUseCase(repo)
        viewModel = MangaRemoteInfoViewModel(observeUseCase, workManager)
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve sincronizar biblioteca ao solicitar`() {
        viewModel.syncLibrary()
        verify { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `deve sincronizar manga especifico do mangadex`() {
        viewModel.syncFromMangadex(1L)
        verify { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `deve sincronizar manga especifico do anilist`() {
        viewModel.syncFromAnilist(1L)
        verify { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `deve refletir progresso do WorkManager`() = runTest {
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.RUNNING
        every { workInfo.progress.getInt(any<String>(), any<Int>()) } returns 45
        
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

        viewModel.syncLibrary()

        viewModel.progress.test {
            assertThat(awaitItem()).isEqualTo(45)
        }
    }
}
