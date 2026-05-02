package br.acerola.comic.usecase.comic

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.error.message.IoError
import br.acerola.comic.service.metadata.CoverExtractor
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

class CoverFromChapterUseCaseTest {
    @MockK
    lateinit var coverExtractor: CoverExtractor

    @MockK
    lateinit var comicGateway: ComicGateway<ComicDirectoryDto>

    private lateinit var useCase: CoverFromChapterUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { comicGateway.progress } returns MutableStateFlow(0)
        every { comicGateway.isIndexing } returns MutableStateFlow(false)
        useCase = CoverFromChapterUseCase(
            coverExtractor = coverExtractor,
            comicGateway = comicGateway,
        )
    }

    // Caminho feliz — extração e refresh funcionam
    @Test
    fun `invoke deve retornar sucesso quando extração e refresh do comic funcionam`() =
        runTest {
            coEvery { coverExtractor.extractFirstPageAsCover(42L) } returns Either.Right(Unit)
            coEvery { comicGateway.refreshManga(42L) } returns Either.Right(Unit)

            val result = useCase(42L)

            assertTrue(result.isRight())
            coVerify(exactly = 1) { coverExtractor.extractFirstPageAsCover(42L) }
            coVerify(exactly = 1) { comicGateway.refreshManga(42L) }
        }

    // Arquivo inexistente → FileNotFound mapeado para LibrarySyncError sem crash
    @Test
    fun `invoke deve retornar erro tipado quando arquivo não existe sem lançar exceção`() =
        runTest {
            coEvery { coverExtractor.extractFirstPageAsCover(42L) } returns
                Either.Left(IoError.FileNotFound("comics/mistery-comic"))

            val result = useCase(42L)

            assertTrue(result.isLeft())
            coVerify(exactly = 1) { coverExtractor.extractFirstPageAsCover(42L) }
            coVerify(exactly = 0) { comicGateway.refreshManga(any()) }
        }

    // Leitura do arquivo falha → FileReadError mapeado para LibrarySyncError sem cast forçado
    @Test
    fun `invoke deve retornar erro tipado quando leitura de arquivo falha sem cast forçado`() =
        runTest {
            val cause = Exception("Disk read error")
            coEvery { coverExtractor.extractFirstPageAsCover(42L) } returns
                Either.Left(IoError.FileReadError("comics/chapter-01.cbz", cause))

            val result = useCase(42L)

            assertTrue(result.isLeft())
            coVerify(exactly = 1) { coverExtractor.extractFirstPageAsCover(42L) }
            coVerify(exactly = 0) { comicGateway.refreshManga(any()) }
        }
}
