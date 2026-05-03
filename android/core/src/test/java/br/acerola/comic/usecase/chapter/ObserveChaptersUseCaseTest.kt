package br.acerola.comic.usecase.chapter

import br.acerola.comic.adapter.contract.gateway.ChapterReadGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.dto.archive.ChapterPageDto
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObserveChaptersUseCaseTest {
    @MockK
    lateinit var readGateway: ChapterReadGateway<ChapterPageDto>

    @MockK
    lateinit var statusGateway: ChapterSyncStatusGateway

    private lateinit var useCase: ObserveChaptersUseCase<ChapterPageDto>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { statusGateway.progress } returns MutableStateFlow(value = 0)
        every { statusGateway.isIndexing } returns MutableStateFlow(value = false)

        useCase = ObserveChaptersUseCase(readGateway = readGateway, syncStatusGateway = statusGateway)
    }

    @Test
    fun `observeByComic deve delegar para o repositorio`() =
        runTest {
            val dto = mockk<ChapterPageDto>()
            every { readGateway.observeChapters(comicId = 1L) } returns MutableStateFlow(value = dto)

            val result = useCase.observeByComic(comicId = 1L).first()

            assertEquals(dto, result)
            coVerify { readGateway.observeChapters(comicId = 1L) }
        }

    @Test
    fun `loadPage deve delegar para o repositorio com parametros corretos`() =
        runTest {
            val dto = mockk<ChapterPageDto>()
            coEvery { readGateway.getChapterPage(comicId = 1L, total = 100, page = 2, pageSize = 20) } returns dto

            val result = useCase.loadPage(comicId = 1L, total = 100, page = 2)

            assertEquals(dto, result)
            coVerify { readGateway.getChapterPage(comicId = 1L, total = 100, page = 2, pageSize = 20) }
        }
}
