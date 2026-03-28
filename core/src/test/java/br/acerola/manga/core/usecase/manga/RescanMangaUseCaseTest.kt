package br.acerola.manga.core.usecase.manga

import arrow.core.Either
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.core.usecase.library.RescanMangaUseCase
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

class RescanMangaUseCaseTest {

    @MockK
    lateinit var repository: MangaGateway<MangaDirectoryDto>

    private lateinit var useCase: RescanMangaUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { repository.progress } returns MutableStateFlow(value = 0)
        every { repository.isIndexing } returns MutableStateFlow(value = false)

        useCase = RescanMangaUseCase(mangaRepository = repository)
    }

    @Test
    fun `invoke deve chamar refreshManga`() = runTest {
        coEvery { repository.refreshManga(mangaId = 1L) } returns Either.Right(value = Unit)

        val result = useCase(mangaId = 1L)

        assertTrue(result.isRight())
        coVerify { repository.refreshManga(mangaId = 1L) }
    }
}
