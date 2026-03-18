package br.acerola.manga.usecase.manga

import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.usecase.library.RescanMangaChaptersUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RescanMangaChaptersUseCaseTest {

    @MockK
    lateinit var repository: ChapterManagementRepository<ChapterArchivePageDto>
    private lateinit var useCase: RescanMangaChaptersUseCase<ChapterArchivePageDto>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { repository.progress } returns MutableStateFlow(value = 0)
        every { repository.isIndexing } returns MutableStateFlow(value = false)
        useCase = RescanMangaChaptersUseCase(chapterRepository = repository)
    }

    @Test
    fun `invoke deve chamar refreshMangaChapters`() = runTest {
        coEvery { repository.refreshMangaChapters(mangaId = 1L) } returns Either.Right(value = Unit)

        val result = useCase(mangaId = 1L)

        assertTrue(result.isRight())
        coVerify { repository.refreshMangaChapters(mangaId = 1L) }
    }
}
