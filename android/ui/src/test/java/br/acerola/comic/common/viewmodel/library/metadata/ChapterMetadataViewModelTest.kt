package br.acerola.comic.common.viewmodel.library.metadata

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.adapter.contract.gateway.ChapterReadGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import io.mockk.coEvery
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
class ChapterMetadataViewModelTest {
    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val readGateway = mockk<ChapterReadGateway<ChapterRemoteInfoPageDto>>(relaxed = true)
    private val statusGateway = mockk<ChapterSyncStatusGateway>(relaxed = true)

    private lateinit var observeChaptersUseCase: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>
    private lateinit var viewModel: ChapterMetadataViewModel

    @Before
    fun setup() {
        every { statusGateway.isIndexing } returns MutableStateFlow(false)
        every { statusGateway.progress } returns MutableStateFlow(-1)

        observeChaptersUseCase = ObserveChaptersUseCase(readGateway = readGateway, syncStatusGateway = statusGateway)
        viewModel = ChapterMetadataViewModel(workManager, observeChaptersUseCase)
    }

    @Test
    fun `deve enfileirar trabalho de sincronizacao do mangadex`() {
        viewModel.syncChaptersByMangadex(1L)
        verify { workManager.enqueueUniqueWork(any(), ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `deve enfileirar trabalho de sincronizacao do comicinfo`() {
        viewModel.syncChaptersByComicInfo(1L)
        verify { workManager.enqueueUniqueWork(any(), ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `deve carregar itens da pagina de metadados`() =
        runTest {
            val remotePage =
                ChapterRemoteInfoPageDto(
                    items =
                        listOf(
                            ChapterFeedDto(1L, "Ch 2", "2", 10, "", emptyList()),
                            ChapterFeedDto(2L, "Ch 1", "1", 10, "", emptyList()),
                        ),
                    pageSize = 20,
                    page = 0,
                    total = 2,
                )

            coEvery { readGateway.getChapterPage(any(), any(), any(), any(), any(), any()) } returns remotePage

            viewModel.init(1L, ChapterRemoteInfoPageDto(emptyList(), 20, 0, 2))
            viewModel.loadPage(0)

            coVerify { readGateway.getChapterPage(1L, 2, 0, 20, "NUMBER", true) }
        }
}
