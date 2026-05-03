package br.acerola.comic.pattern

import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.infra.R
import br.acerola.comic.pattern.template.TemplateValidator
import br.acerola.comic.util.sort.SortType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateValidatorTest {
    @Test
    fun `deve validar com sucesso um padrao perfeito de capitulo`() {
        val input = "Cap. {chapter}{decimal} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.CHAPTER)
        assertTrue(result.isRight())
    }

    @Test
    fun `deve validar com sucesso um padrao perfeito de volume`() {
        val input = "Vol. {volume} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.VOLUME)
        assertTrue(result.isRight())
    }

    @Test
    fun `deve falhar se nao houver macro chapter no modo capitulo`() {
        val input = "Cap. {decimal} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.CHAPTER)

        assertTrue(result.isLeft())
        result.onLeft {
            assertEquals(R.string.error_template_chapter_required, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }
    }

    @Test
    fun `deve falhar se nao houver macro volume no modo volume`() {
        val input = "Vol. {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.VOLUME)

        assertTrue(result.isLeft())
        result.onLeft {
            assertEquals(R.string.error_template_volume_required, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }
    }

    @Test
    fun `deve falhar se houver chapter no modo volume`() {
        // Agora chapter no modo volume deve falhar pois volume é o obrigatório
        val input = "Vol. {chapter} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.VOLUME)

        assertTrue(result.isLeft())
        result.onLeft {
            assertEquals(R.string.error_template_volume_required, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }
    }

    @Test
    fun `deve falhar se houver mais de um sub`() {
        val input = "{chapter}{decimal}{decimal}{extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.CHAPTER)

        assertTrue(result.isLeft())
        result.onLeft {
            assertEquals(R.string.error_template_decimal_duplicate, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }
    }

    @Test
    fun `deve falhar se a extensao nao for a ultima macro ou estiver ausente`() {
        val input = "{chapter}{decimal}"
        val result1 = TemplateValidator.validateCustomTemplate(input, SortType.CHAPTER)

        assertTrue(result1.isLeft())
        result1.onLeft {
            assertEquals(R.string.error_template_extension_required, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }

        val input2 = "{chapter}{extension} Lixo"
        val result2 = TemplateValidator.validateCustomTemplate(input2, SortType.CHAPTER)

        assertTrue(result2.isLeft())
        result2.onLeft {
            assertEquals(R.string.error_template_extension_at_end, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }
    }

    @Test
    fun `deve falhar se a ordem estiver incorreta`() {
        val wrongSub = "{decimal}{chapter}{extension}"
        val res1 = TemplateValidator.validateCustomTemplate(wrongSub, SortType.CHAPTER)
        assertTrue(res1.isLeft())
        res1.onLeft { assertEquals(R.string.error_template_chapter_before_decimal, (it as TemplateError.InvalidPattern).uiMessage.resId) }

        val wrongExt = "{extension}{chapter}"
        val res2 = TemplateValidator.validateCustomTemplate(wrongExt, SortType.CHAPTER)
        assertTrue(res2.isLeft())
        res2.onLeft { assertEquals(R.string.error_template_chapter_before_extension, (it as TemplateError.InvalidPattern).uiMessage.resId) }
    }

    @Test
    fun `deve falhar com macros malformadas ou invalidas`() {
        val malformed = "{value"
        val res1 = TemplateValidator.validateCustomTemplate(malformed, SortType.CHAPTER)
        assertTrue(res1.isLeft())
        res1.onLeft { assertEquals(R.string.error_template_malformed_macro, (it as TemplateError.InvalidPattern).uiMessage.resId) }

        val invalid = "{batata}"
        val res2 = TemplateValidator.validateCustomTemplate(invalid, SortType.CHAPTER)
        assertTrue(res2.isLeft())
        res2.onLeft {
            val error = it as TemplateError.InvalidPattern
            assertEquals(R.string.error_template_invalid_macro, error.uiMessage.resId)
            assertEquals("batata", error.uiMessage.args.first())
        }
    }
}
