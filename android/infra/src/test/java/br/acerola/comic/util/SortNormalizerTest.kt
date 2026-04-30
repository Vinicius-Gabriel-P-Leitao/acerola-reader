package br.acerola.comic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SortNormalizerTest {
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
            val result = SortNormalizer.normalize(input, SortType.VOLUME)
            assertEquals("Failed for input: $input", expected, result.normalizedSort)
        }
    }

    @Test
    fun `should normalize chapters correctly`() {
        val cases =
            mapOf(
                "Cap 01" to "1",
                "Chapter 10.5" to "10.5",
                "1.10" to "1.10",
                "001" to "1",
            )

        cases.forEach { (input, expected) ->
            val result = SortNormalizer.normalize(input, SortType.CHAPTER)
            assertEquals("Failed for input: $input", expected, result.normalizedSort)
        }
    }

    @Test
    fun `should detect special archives`() {
        val specials = listOf("Oneshot", "Special 1", "Extra story", "Especial 2")

        specials.forEach { input ->
            val result = SortNormalizer.normalize(input, SortType.CHAPTER)
            assertEquals("Should be special: $input", true, result.isSpecial)
        }
    }

    @Test
    fun `should use fallback index when no number is found`() {
        val result = SortNormalizer.normalize("Unknown", SortType.CHAPTER, fallbackIndex = 99)
        assertEquals("99", result.normalizedSort)
        assertEquals(99, result.integerPart)
    }

    @Test
    fun `should handle decimal part correctly for ordering`() {
        val v1 = SortNormalizer.normalize("Vol 1.1", SortType.VOLUME)
        val v10 = SortNormalizer.normalize("Vol 1.10", SortType.VOLUME)

        assertEquals(1, v1.decimalPart)
        assertEquals(10, v10.decimalPart)
    }
}
