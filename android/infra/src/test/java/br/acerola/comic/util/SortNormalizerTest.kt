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
                "Ch. 0.01.cbz" to "0.01",
                "Ch. 0.02.cbz" to "0.02",
                "Ch. 0.10.cbz" to "0.10",
            )

        cases.forEach { (input, expected) ->
            val result = SortNormalizer.normalize(input, SortType.CHAPTER, chapterTemplates)
            assertEquals("Failed for input: $input", expected, result.normalizedSort)
        }
    }

    @Test
    fun `should detect special archives only when no numbers exist`() {
        val specials = listOf("Oneshot", "Extra story")

        specials.forEach { input ->
            val result = SortNormalizer.normalize(input, SortType.CHAPTER, chapterTemplates)
            assertEquals("Should be special: $input", true, result.isSpecial)
        }

        val nonSpecials = listOf("Ch. 3.5 - Extras.cbz", "Special 1.cbz")
        nonSpecials.forEach { input ->
            val result = SortNormalizer.normalize(input, SortType.CHAPTER, chapterTemplates)
            assertEquals("Should NOT be special: $input", false, result.isSpecial)
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

    @Test
    fun `should sort decimal chapters in strict numerical order`() {
        val rawList = listOf(
            "Capitulo 41.cbz",
            "Capitulo 3.5.cbz",
            "Capitulo 10.5.cbz",
            "Ch. 0.01.cbz",
            "Ch. 0.02.cbz",
            "Capitulo 1.cbz",
            "Capitulo 3.cbz",
        )

        val normalized = rawList.map { name ->
            name to SortNormalizer.normalize(name, SortType.CHAPTER, chapterTemplates)
        }

        val sortedNames = normalized.sortedBy { (_, result) ->
            result.normalizedSort.toDoubleOrNull() ?: 0.0
        }.map { it.first }

        val expectedOrder = listOf(
            "Ch. 0.01.cbz",
            "Ch. 0.02.cbz",
            "Capitulo 1.cbz",
            "Capitulo 3.cbz",
            "Capitulo 3.5.cbz",
            "Capitulo 10.5.cbz",
            "Capitulo 41.cbz",
        )

        assertEquals(expectedOrder, sortedNames)
    }
}
