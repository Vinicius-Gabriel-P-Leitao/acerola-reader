package br.acerola.manga.pattern

import br.acerola.manga.error.message.TemplateError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateValidatorPatternTest {

    @Test
    fun `deve validar com sucesso um padrao perfeito`() {
        val input = "Cap. {chapter}{decimal} - {extension}"
        val result = TemplateValidatorPattern.validateCustomTemplate(input)
        assertTrue(result.isRight())
    }

    @Test
    fun `deve falhar se nao houver macro value`() {
        val input = "Cap. {decimal} - {extension}"
        val result = TemplateValidatorPattern.validateCustomTemplate(input)
        
        assertTrue(result.isLeft())
        result.onLeft { 
            assertEquals("Exactly one {chapter} is required", (it as TemplateError.InvalidPattern).reason)
        }
    }

    @Test
    fun `deve falhar se houver mais de um sub`() {
        val input = "{chapter}{decimal}{decimal}{extension}"
        val result = TemplateValidatorPattern.validateCustomTemplate(input)
        
        assertTrue(result.isLeft())
        result.onLeft { 
            assertEquals("Only one {decimal} is allowed", (it as TemplateError.InvalidPattern).reason)
        }
    }

    @Test
    fun `deve falhar se a extensao nao for a ultima macro ou estiver ausente`() {
        val input = "{chapter}{decimal}"
        val result1 = TemplateValidatorPattern.validateCustomTemplate(input)
        
        assertTrue(result1.isLeft())
        result1.onLeft { 
            assertEquals("Exactly one {extension} is required", (it as TemplateError.InvalidPattern).reason)
        }

        val input2 = "{chapter}{extension} Lixo"
        val result2 = TemplateValidatorPattern.validateCustomTemplate(input2)
        
        assertTrue(result2.isLeft())
        result2.onLeft { 
            assertEquals("{extension} must be at the end of the pattern", (it as TemplateError.InvalidPattern).reason)
        }
    }

    @Test
    fun `deve falhar se a ordem estiver incorreta`() {
        val wrongSub = "{decimal}{chapter}{extension}"
        val res1 = TemplateValidatorPattern.validateCustomTemplate(wrongSub)
        assertTrue(res1.isLeft())
        res1.onLeft { assertEquals("{chapter} must come before {decimal}", (it as TemplateError.InvalidPattern).reason) }

        val wrongExt = "{extension}{chapter}"
        val res2 = TemplateValidatorPattern.validateCustomTemplate(wrongExt)
        assertTrue(res2.isLeft())
        res2.onLeft { assertEquals("{chapter} must come before {extension}", (it as TemplateError.InvalidPattern).reason) }
    }

    @Test
    fun `deve falhar com macros malformadas ou invalidas`() {
        val malformed = "{value"
        val res1 = TemplateValidatorPattern.validateCustomTemplate(malformed)
        assertTrue(res1.isLeft())
        res1.onLeft { assertEquals("Malformed macro", (it as TemplateError.InvalidPattern).reason) }

        val invalid = "{batata}"
        val res2 = TemplateValidatorPattern.validateCustomTemplate(invalid)
        assertTrue(res2.isLeft())
        res2.onLeft { assertEquals("Invalid macro: batata", (it as TemplateError.InvalidPattern).reason) }
    }
}