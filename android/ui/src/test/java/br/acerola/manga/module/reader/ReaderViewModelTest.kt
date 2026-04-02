package br.acerola.manga.module.reader

import android.content.Context
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.gateway.HistoryGateway
import br.acerola.manga.core.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.core.usecase.history.TrackReadingProgressUseCase
import br.acerola.manga.service.reader.ReaderProcessor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val readerService = mockk<ReaderProcessor>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val historyGateway = mockk<HistoryGateway>(relaxed = true)
    private val chapterRepo = mockk<ChapterGateway<ChapterArchivePageDto>>(relaxed = true)

    private lateinit var trackReadingProgressUseCase: TrackReadingProgressUseCase
    private lateinit var observeChaptersUseCase: ObserveChaptersUseCase<ChapterArchivePageDto>
    
    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setup() {
        every { chapterRepo.isIndexing } returns MutableStateFlow(false)
        every { chapterRepo.progress } returns MutableStateFlow(-1)

        trackReadingProgressUseCase = TrackReadingProgressUseCase(historyGateway)
        observeChaptersUseCase = ObserveChaptersUseCase(chapterRepo)

        viewModel = ReaderViewModel(
            repository = readerService,
            context = context,
            trackReadingProgressUseCase = trackReadingProgressUseCase,
            observeChaptersUseCase = observeChaptersUseCase
        )
    }

    @Test
    fun `deve inicializar com estado inicial`() {
        assert(!viewModel.state.value.isLoading)
    }
}
