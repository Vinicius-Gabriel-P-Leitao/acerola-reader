package br.acerola.comic.module.reader

import android.content.Context
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.history.TrackReadingProgressUseCase
import br.acerola.comic.service.reader.ReaderProcessor
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
