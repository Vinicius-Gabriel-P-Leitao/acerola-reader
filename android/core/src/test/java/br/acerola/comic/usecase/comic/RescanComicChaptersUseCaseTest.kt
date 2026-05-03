package br.acerola.comic.usecase.comic

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ChapterSyncGateway
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
    lateinit var repository: ChapterSyncGateway
    private lateinit var useCase: RescanComicChaptersUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { repository.progress } returns MutableStateFlow(value = 0)
        every { repository.isIndexing } returns MutableStateFlow(value = false)
        useCase = RescanComicChaptersUseCase(chapterRepository = repository)
    }

    @Test
    fun invokeDeveChamarRefreshMangaChapters() =
        runTest {
            coEvery { repository.refreshComicChapters(comicId = 1L) } returns Either.Right(value = Unit)

            val result = useCase(comicId = 1L)

            assertTrue(result.isRight())
            coVerify { repository.refreshComicChapters(comicId = 1L) }
        }
}
