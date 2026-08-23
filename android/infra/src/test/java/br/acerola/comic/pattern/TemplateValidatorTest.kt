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
    fun `should validate successfully a perfect chapter pattern`() {
        val input = "Cap. {chapter}{decimal} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.CHAPTER)
        assertTrue(result.isRight())
    }

    @Test
    fun `should validate successfully a perfect volume pattern`() {
        val input = "Vol. {volume} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.VOLUME)
        assertTrue(result.isRight())
    }

    @Test
    fun `should fail if there is no chapter macro in chapter mode`() {
        val input = "Cap. {decimal} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.CHAPTER)

        assertTrue(result.isLeft())
        result.onLeft {
            assertEquals(R.string.error_template_chapter_required, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }
    }

    @Test
    fun `should fail if there is no volume macro in volume mode`() {
        val input = "Vol. {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.VOLUME)

        assertTrue(result.isLeft())
        result.onLeft {
            assertEquals(R.string.error_template_volume_required, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }
    }

    @Test
    fun `should fail if there is chapter in volume mode`() {
        // Agora chapter no modo volume deve falhar pois volume é o obrigatório
        val input = "Vol. {chapter} - {extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.VOLUME)

        assertTrue(result.isLeft())
        result.onLeft {
            assertEquals(R.string.error_template_volume_required, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }
    }

    @Test
    fun `should fail if there is more than one decimal`() {
        val input = "{chapter}{decimal}{decimal}{extension}"
        val result = TemplateValidator.validateCustomTemplate(input, SortType.CHAPTER)

        assertTrue(result.isLeft())
        result.onLeft {
            assertEquals(R.string.error_template_decimal_duplicate, (it as TemplateError.InvalidPattern).uiMessage.resId)
        }
    }

    @Test
    fun `should fail if extension is not the last macro or is absent`() {
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
    fun `should fail if order is incorrect`() {
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
    fun `should fail with malformed or invalid macros`() {
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
