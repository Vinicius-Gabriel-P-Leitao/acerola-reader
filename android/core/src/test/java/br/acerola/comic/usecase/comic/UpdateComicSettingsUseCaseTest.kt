package br.acerola.comic.usecase.comic

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicLibraryWriteGateway
import br.acerola.comic.error.message.LibrarySyncError
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateComicSettingsUseCaseTest {
    @MockK
    lateinit var gateway: ComicLibraryWriteGateway

    private lateinit var useCase: UpdateComicSettingsUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = UpdateComicSettingsUseCase(gateway = gateway)
    }

    // Caminho feliz — sync externo habilitado
    @Test
    fun `invoke deve retornar sucesso e chamar gateway exatamente uma vez ao habilitar sync`() =
        runTest {
            coEvery { gateway.updateMangaSettings(any(), any()) } returns Either.Right(Unit)

            val result = useCase(comicId = 5L, externalSyncEnabled = true)

            assertTrue(result.isRight())
            coVerify(exactly = 1) { gateway.updateMangaSettings(5L, true) }
        }

    // Caminho feliz — sync externo desabilitado
    @Test
    fun `invoke deve retornar sucesso e chamar gateway exatamente uma vez ao desabilitar sync`() =
        runTest {
            coEvery { gateway.updateMangaSettings(any(), any()) } returns Either.Right(Unit)

            val result = useCase(comicId = 5L, externalSyncEnabled = false)

            assertTrue(result.isRight())
            coVerify(exactly = 1) { gateway.updateMangaSettings(5L, false) }
        }

    // Erro de banco de dados → retorna Left sem lançar exceção
    @Test
    fun `invoke deve retornar erro tipado quando gateway falha sem lançar exceção`() =
        runTest {
            coEvery { gateway.updateMangaSettings(any(), any()) } returns
                Either.Left(LibrarySyncError.DatabaseError())

            val result = useCase(comicId = 5L, externalSyncEnabled = true)

            assertTrue(result.isLeft())
            coVerify(exactly = 1) { gateway.updateMangaSettings(5L, true) }
        }
}
