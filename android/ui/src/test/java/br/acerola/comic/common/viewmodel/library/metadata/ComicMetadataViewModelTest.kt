package br.acerola.comic.common.viewmodel.library.metadata

import androidx.work.WorkManager
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComicMetadataViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val mangaRepo = mockk<ComicGateway<ComicMetadataDto>>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)
    
    private lateinit var observeLibraryUseCase: ObserveLibraryUseCase<ComicMetadataDto>
    private lateinit var viewModel: ComicMetadataViewModel

    @Before
    fun setup() {
        every { mangaRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { mangaRepo.isIndexing } returns MutableStateFlow(false)
        every { mangaRepo.progress } returns MutableStateFlow(-1)

        observeLibraryUseCase = ObserveLibraryUseCase(mangaRepository = mangaRepo)
        viewModel = ComicMetadataViewModel(observeLibraryUseCase, manageCategoriesUseCase, workManager)
    }

    @Test
    fun `deve inicializar com valores padrao`() {
        assert(!viewModel.isIndexing.value)
    }
}
