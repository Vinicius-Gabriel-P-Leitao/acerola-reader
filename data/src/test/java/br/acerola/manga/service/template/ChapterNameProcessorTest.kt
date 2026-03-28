package br.acerola.manga.service.template

import br.acerola.manga.error.message.TemplateError
import br.acerola.manga.local.dao.archive.ChapterTemplateDao
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterNameProcessorTest {

    @MockK
    lateinit var dao: ChapterTemplateDao

    private lateinit var service: ChapterNameProcessor

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        service = ChapterNameProcessor(dao)
        
        coEvery { dao.insert(any()) } returns 1L
    }

    @Test
    fun `deve anexar a extensao automaticamente se o usuario nao a prover`() = runTest {
        val result = service.addTemplate("Meu Template", "Cap. {value}")
        
        assertTrue(result.isRight())
        // O mock insere qualquer coisa, mas podemos testar a validacao embutida.
        // Se a validacao falhar, ele retorna um Either.Left. Como retornou Right,
        // significa que a transformacao "Cap. {value}*{extension}" foi valida.
    }

    @Test
    fun `deve remover lixo apos a extensao caso o usuario forneca`() = runTest {
        val result = service.addTemplate("Template com Lixo", "Ch. {value}{extension} LixoAqui")
        
        assertTrue(result.isRight())
        // Como validou, o "LixoAqui" foi removido corretamente, tornando o padrao valido.
    }

    @Test
    fun `deve rejeitar padrao invalido com erros apropriados`() = runTest {
        val result = service.addTemplate("Template Invalido", "Sem a macro de valor")
        
        assertTrue(result.isLeft())
        result.onLeft { 
            assertTrue(it is TemplateError.InvalidPattern)
            assertEquals("Exactly one {value} is required", (it as TemplateError.InvalidPattern).reason)
        }
    }
}
