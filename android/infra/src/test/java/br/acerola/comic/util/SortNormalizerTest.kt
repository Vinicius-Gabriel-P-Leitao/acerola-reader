package br.acerola.comic.util

import br.acerola.comic.pattern.template.TemplateMacro
import br.acerola.comic.util.sort.SortNormalizer
import br.acerola.comic.util.sort.SortType
import org.junit.Assert.assertEquals
import org.junit.Test

class SortNormalizerTest {
    private val volumeTemplates =
        listOf(
            "Vol. {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Volume {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "V{${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Edicao {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Edição {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Vol {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "V {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
        )

    private val chapterTemplates =
        listOf(
            "{${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
            "Ch. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
            "Cap. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
            "Cap {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
            "Chapter {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
            "chapter {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
        )

    @Test
    fun `should normalize volumes correctly`() {
        val cases =
            mapOf(
                "Vol 1" to "1",
                "Volume 02" to "2",
                "V 3.5" to "3.5",
                "Vol. 10" to "10",
                "Volume 1.10" to "1.10",
                "Edicao 5" to "5",
                "Edição 06" to "6",
            )

        cases.forEach { (input, expected) ->
            val result = SortNormalizer.normalize(input, SortType.VOLUME, volumeTemplates)
            assertEquals("Failed for input: $input", expected, result.normalizedSort)
        }
    }

    @Test
    fun `should normalize chapters correctly`() {
        val cases =
            mapOf(
                "Cap 01.cbz" to "1",
                "Chapter 10.5.cbz" to "10.5",
                "1.10.cbz" to "1.10",
                "001.cbz" to "1",
            )

        cases.forEach { (input, expected) ->
            val result = SortNormalizer.normalize(input, SortType.CHAPTER, chapterTemplates)
            assertEquals("Failed for input: $input", expected, result.normalizedSort)
        }
    }

    @Test
    fun `should detect special archives`() {
        val specials = listOf("Oneshot", "Special 1", "Extra story", "Especial 2")

        specials.forEach { input ->
            val result = SortNormalizer.normalize(input, SortType.CHAPTER, chapterTemplates)
            assertEquals("Should be special: $input", true, result.isSpecial)
        }
    }

    @Test
    fun `should use fallback index when no number is found`() {
        val result = SortNormalizer.normalize("Unknown", SortType.CHAPTER, chapterTemplates, fallbackIndex = 99)
        assertEquals("99", result.normalizedSort)
        assertEquals(99, result.integerPart)
    }

    @Test
    fun `should handle decimal part correctly for ordering`() {
        val v1 = SortNormalizer.normalize("Vol 1.1", SortType.VOLUME, volumeTemplates)
        val v10 = SortNormalizer.normalize("Vol 1.10", SortType.VOLUME, volumeTemplates)

        assertEquals(1, v1.decimalPart)
        assertEquals(10, v10.decimalPart)
    }
}
