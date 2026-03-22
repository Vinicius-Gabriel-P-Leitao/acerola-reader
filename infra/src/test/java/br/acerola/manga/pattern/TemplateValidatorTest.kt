package br.acerola.manga.pattern

import br.acerola.manga.error.message.TemplateError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateValidatorTest {

    @Test
    fun `deve validar com sucesso um padrao perfeito`() {
        val input = "Cap. {value}{sub} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input)
        assertTrue(result.isRight())
    }

    @Test
    fun `deve falhar se nao houver macro value`() {
        val input = "Cap. {sub} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input)
        
        assertTrue(result.isLeft())
        result.onLeft { 
            assertEquals("Exactly one {value} is required", (it as TemplateError.InvalidPattern).reason)
        }
    }

    @Test
    fun `deve falhar se houver mais de um sub`() {
        val input = "{value}{sub}{sub}{extension}"
        val result = TemplateValidator.validateCustomTemplate(input)
        
        assertTrue(result.isLeft())
        result.onLeft { 
            assertEquals("Only one {sub} is allowed", (it as TemplateError.InvalidPattern).reason)
        }
    }

    @Test
    fun `deve falhar se a extensao nao for a ultima macro ou estiver ausente`() {
        val input = "{value}{sub}"
        val result1 = TemplateValidator.validateCustomTemplate(input)
        
        assertTrue(result1.isLeft())
        result1.onLeft { 
            assertEquals("Exactly one {extension} is required", (it as TemplateError.InvalidPattern).reason)
        }

        val input2 = "{value}{extension} Lixo"
        val result2 = TemplateValidator.validateCustomTemplate(input2)
        
        assertTrue(result2.isLeft())
        result2.onLeft { 
            assertEquals("{extension} must be at the end of the pattern", (it as TemplateError.InvalidPattern).reason)
        }
    }

    @Test
    fun `deve falhar se a ordem estiver incorreta`() {
        val wrongSub = "{sub}{value}{extension}"
        val res1 = TemplateValidator.validateCustomTemplate(wrongSub)
        assertTrue(res1.isLeft())
        res1.onLeft { assertEquals("{value} must come before {sub}", (it as TemplateError.InvalidPattern).reason) }

        val wrongExt = "{extension}{value}"
        val res2 = TemplateValidator.validateCustomTemplate(wrongExt)
        assertTrue(res2.isLeft())
        res2.onLeft { assertEquals("{value} must come before {extension}", (it as TemplateError.InvalidPattern).reason) }
    }

    @Test
    fun `deve falhar com macros malformadas ou invalidas`() {
        val malformed = "{value"
        val res1 = TemplateValidator.validateCustomTemplate(malformed)
        assertTrue(res1.isLeft())
        res1.onLeft { assertEquals("Malformed macro", (it as TemplateError.InvalidPattern).reason) }

        val invalid = "{batata}"
        val res2 = TemplateValidator.validateCustomTemplate(invalid)
        assertTrue(res2.isLeft())
        res2.onLeft { assertEquals("Invalid macro: batata", (it as TemplateError.InvalidPattern).reason) }
    }
}