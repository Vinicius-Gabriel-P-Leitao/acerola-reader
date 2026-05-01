package br.acerola.comic.usecase.comic

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.usecase.library.RescanComicUseCase
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

class RescanComicUseCaseTest {
    @MockK
    lateinit var repository: ComicGateway<ComicDirectoryDto>

    private lateinit var useCase: RescanComicUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { repository.progress } returns MutableStateFlow(value = 0)
        every { repository.isIndexing } returns MutableStateFlow(value = false)

        useCase = RescanComicUseCase(comicRepository = repository)
    }

    @Test
    fun invokeDeveChamarRefreshManga() =
        runTest {
            coEvery { repository.refreshManga(comicId = 1L) } returns Either.Right(value = Unit)

            val result = useCase(comicId = 1L)

            assertTrue(result.isRight())
            coVerify { repository.refreshManga(comicId = 1L) }
        }
}
