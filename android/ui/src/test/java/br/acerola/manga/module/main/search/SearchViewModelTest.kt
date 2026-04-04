package br.acerola.manga.module.main.search

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import arrow.core.Either
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.core.usecase.search.SearchMangaUseCase
import br.acerola.manga.core.worker.ChapterDownloadWorker
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.module.main.search.state.SearchAction
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val searchMangaUseCase = mockk<SearchMangaUseCase>()
    private val workManager = mockk<WorkManager>(relaxed = true)

    private val workInfosFlow = MutableStateFlow<List<WorkInfo>>(emptyList())

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        every { workManager.getWorkInfosByTagFlow(any()) } returns workInfosFlow
        viewModel = SearchViewModel(context, searchMangaUseCase, workManager)
    }

    @Test
    fun `QueryChanged atualiza query no estado`() = runTest {
        viewModel.onAction(SearchAction.QueryChanged("Naruto"))

        viewModel.uiState.test {
            assertThat(awaitItem().query).isEqualTo("Naruto")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Search com query em branco nao dispara busca`() = runTest {
        viewModel.onAction(SearchAction.QueryChanged("   "))
        viewModel.onAction(SearchAction.Search)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.searchResults).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Search com sucesso atualiza resultados e limpa loading`() = runTest {
        val manga = mockk<MangaMetadataDto>()
        coEvery { searchMangaUseCase.search("Naruto") } returns Either.Right(listOf(manga))

        viewModel.onAction(SearchAction.QueryChanged("Naruto"))
        viewModel.onAction(SearchAction.Search)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.searchResults).containsExactly(manga)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Search com erro emite evento de erro e limpa loading`() = runTest {
        coEvery { searchMangaUseCase.search("Naruto") } returns Either.Left(NetworkError.ConnectionFailed())

        viewModel.onAction(SearchAction.QueryChanged("Naruto"))
        viewModel.onAction(SearchAction.Search)

        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.uiEvents.test {
            assertThat(awaitItem()).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fila de download e populada com trabalhos em andamento`() = runTest {
        val progressData = mockk<androidx.work.Data> {
            every { getInt("progress", 0) } returns 3
            every { getInt("totalChapters", 0) } returns 10
            every { getString("currentChapterId") } returns null
            every { getString("currentChapterFileName") } returns null
        }
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.RUNNING
            every { tags } returns setOf(ChapterDownloadWorker.DOWNLOAD_TAG, "chapter_download_Naruto")
            every { progress } returns progressData
        }

        workInfosFlow.value = listOf(workInfo)

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.downloadQueue.isEmpty()) state = awaitItem()
            assertThat(state.downloadQueue).hasSize(1)
            assertThat(state.downloadQueue[0].mangaTitle).isEqualTo("Naruto")
            assertThat(state.downloadQueue[0].progress).isEqualTo(3)
            assertThat(state.downloadQueue[0].totalChapters).isEqualTo(10)
            assertThat(state.downloadQueue[0].isRunning).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fila de download ignora trabalhos concluidos`() = runTest {
        val progressData = mockk<androidx.work.Data>(relaxed = true)
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.SUCCEEDED
            every { tags } returns setOf(ChapterDownloadWorker.DOWNLOAD_TAG)
            every { progress } returns progressData
        }

        workInfosFlow.value = listOf(workInfo)

        viewModel.uiState.test {
            assertThat(awaitItem().downloadQueue).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
