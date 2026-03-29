package br.acerola.manga.common.viewmodel.library.metadata

import androidx.work.WorkManager
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.core.usecase.metadata.ManageCategoriesUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MangaMetadataViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val mangaRepo = mockk<MangaGateway<MangaMetadataDto>>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val manageCategoriesUseCase = mockk<ManageCategoriesUseCase>(relaxed = true)
    
    private lateinit var observeLibraryUseCase: ObserveLibraryUseCase<MangaMetadataDto>
    private lateinit var viewModel: MangaMetadataViewModel

    @Before
    fun setup() {
        every { mangaRepo.observeLibrary() } returns MutableStateFlow(emptyList())
        every { mangaRepo.isIndexing } returns MutableStateFlow(false)
        every { mangaRepo.progress } returns MutableStateFlow(-1)

        observeLibraryUseCase = ObserveLibraryUseCase(mangaRepository = mangaRepo)
        viewModel = MangaMetadataViewModel(observeLibraryUseCase, manageCategoriesUseCase, workManager)
    }

    @Test
    fun `deve inicializar com valores padrao`() {
        assert(!viewModel.isIndexing.value)
    }
}
