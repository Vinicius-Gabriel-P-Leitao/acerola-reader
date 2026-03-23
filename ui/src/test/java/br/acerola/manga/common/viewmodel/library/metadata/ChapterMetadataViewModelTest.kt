package br.acerola.manga.common.viewmodel.library.metadata

import androidx.work.WorkManager
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.core.usecase.chapter.ObserveChaptersUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterMetadataViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val chapterRepo = mockk<ChapterGateway<ChapterRemoteInfoPageDto>>(relaxed = true)
    
    private lateinit var observeChaptersUseCase: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>
    private lateinit var viewModel: ChapterMetadataViewModel

    @Before
    fun setup() {
        every { chapterRepo.isIndexing } returns MutableStateFlow(false)
        every { chapterRepo.progress } returns MutableStateFlow(-1)

        observeChaptersUseCase = ObserveChaptersUseCase(chapterRepo)
        viewModel = ChapterMetadataViewModel(workManager, observeChaptersUseCase)
    }

    @Test
    fun `deve inicializar com valores padrao`() {
        assert(!viewModel.isIndexing.value)
    }
}
