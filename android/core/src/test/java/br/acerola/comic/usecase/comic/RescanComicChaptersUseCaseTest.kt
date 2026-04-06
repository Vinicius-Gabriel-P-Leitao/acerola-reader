package br.acerola.comic.usecase.comic

import arrow.core.Either
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.usecase.library.RescanComicChaptersUseCase
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

class RescanComicChaptersUseCaseTest {

    @MockK
    lateinit var repository: ChapterGateway<ChapterArchivePageDto>
    private lateinit var useCase: RescanComicChaptersUseCase<ChapterArchivePageDto>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { repository.progress } returns MutableStateFlow(value = 0)
        every { repository.isIndexing } returns MutableStateFlow(value = false)
        useCase = RescanComicChaptersUseCase(chapterRepository = repository)
    }

    @Test
    fun invokeDeveChamarRefreshMangaChapters() = runTest {
        coEvery { repository.refreshComicChapters(mangaId = 1L) } returns Either.Right(value = Unit)

        val result = useCase(mangaId = 1L)

        assertTrue(result.isRight())
        coVerify { repository.refreshComicChapters(mangaId = 1L) }
    }
}
