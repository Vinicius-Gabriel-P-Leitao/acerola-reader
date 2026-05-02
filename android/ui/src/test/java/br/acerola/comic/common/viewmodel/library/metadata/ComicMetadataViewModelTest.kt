package br.acerola.comic.common.viewmodel.library.metadata

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComicMetadataViewModelTest {
    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val comicRepo = mockk<ComicGateway<ComicMetadataDto>>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)

    private lateinit var observeLibraryUseCase: ObserveLibraryUseCase<ComicMetadataDto>
    private lateinit var viewModel: ComicMetadataViewModel

    @Before
    fun setup() {
        every { comicRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { comicRepo.isIndexing } returns MutableStateFlow(false)
        every { comicRepo.progress } returns MutableStateFlow(-1)

        observeLibraryUseCase = ObserveLibraryUseCase(comicRepository = comicRepo)
        viewModel = ComicMetadataViewModel(workManager, manageCategoriesUseCase, observeLibraryUseCase)
    }

    @Test
    fun `deve chamar use case para criar categoria`() =
        runTest {
            viewModel.createCategory("Nova", 0xFF0000)
            coVerify { manageCategoriesUseCase.createCategory("Nova", 0xFF0000) }
        }

    @Test
    fun `deve enfileirar sincronizacao do mangadex`() {
        viewModel.syncFromMangadex(1L)
        verify { workManager.enqueueUniqueWork(any(), ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `deve enfileirar sincronizacao do anilist`() {
        viewModel.syncFromAnilist(1L)
        verify { workManager.enqueueUniqueWork(any(), ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>()) }
    }
}
