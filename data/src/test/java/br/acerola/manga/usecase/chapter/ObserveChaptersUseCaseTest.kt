package br.acerola.manga.usecase.chapter

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.adapter.contract.ChapterPort
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
    lateinit var repository: ChapterPort<ChapterArchivePageDto>

    private lateinit var useCase: ObserveChaptersUseCase<ChapterArchivePageDto>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { repository.progress } returns MutableStateFlow(value = 0)
        every { repository.isIndexing } returns MutableStateFlow(value = false)

        useCase = ObserveChaptersUseCase(chapterRepository = repository)
    }

    @Test
    fun `observeByManga deve delegar para o repositorio`() = runTest {
        val dto = mockk<ChapterArchivePageDto>()
        every { repository.observeChapters(mangaId = 1L) } returns MutableStateFlow(value = dto)

        val result = useCase.observeByManga(mangaId = 1L).first()

        assertEquals(dto, result)
        coVerify { repository.observeChapters(mangaId = 1L) }
    }

    @Test
    fun `loadPage deve delegar para o repositorio com parametros corretos`() = runTest {
        val dto = mockk<ChapterArchivePageDto>()
        coEvery { repository.getChapterPage(mangaId = 1L, total = 100, page = 2, pageSize = 20) } returns dto

        val result = useCase.loadPage(mangaId = 1L, total = 100, page = 2)

        assertEquals(dto, result)
        coVerify { repository.getChapterPage(mangaId = 1L, total = 100, page = 2, pageSize = 20) }
    }
}
