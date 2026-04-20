package br.acerola.comic.usecase.template

import arrow.core.Either
import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.service.template.ChapterNameProcessor
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateTemplateUseCaseTest {
    @MockK
    lateinit var service: ChapterNameProcessor

    private lateinit var useCase: UpdateTemplateUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = UpdateTemplateUseCase(service)
    }

    @Test
    fun `delega para o service com os parametros corretos`() =
        runTest {
            coEvery { service.updateTemplate(1L, "Label", "{chapter}") } returns Either.Right(Unit)

            useCase(1L, "Label", "{chapter}")

            coVerify { service.updateTemplate(1L, "Label", "{chapter}") }
        }

    @Test
    fun `retorna Right quando service tem sucesso`() =
        runTest {
            coEvery { service.updateTemplate(any(), any(), any()) } returns Either.Right(Unit)

            val result = useCase(1L, "Label", "{chapter}")

            assertTrue(result.isRight())
        }

    @Test
    fun `propaga Left quando service retorna Duplicate`() =
        runTest {
            coEvery { service.updateTemplate(any(), any(), any()) } returns Either.Left(TemplateError.Duplicate)

            val result = useCase(1L, "Label", "{chapter}")

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is TemplateError.Duplicate) }
        }

    @Test
    fun `propaga Left quando service retorna SystemProtected`() =
        runTest {
            coEvery { service.updateTemplate(any(), any(), any()) } returns Either.Left(TemplateError.SystemProtected)

            val result = useCase(99L, "Label", "{chapter}")

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is TemplateError.SystemProtected) }
        }
}
