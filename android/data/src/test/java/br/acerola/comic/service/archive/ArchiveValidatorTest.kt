package br.acerola.comic.service.archive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArchiveValidatorTest {
    private lateinit var validator: ArchiveValidator

    @Before
    fun setUp() {
        validator = ArchiveValidator()
    }

    @Test
    fun `isPdfConversionEligible deve retornar verdadeiro se o CBZ de destino nao existir e corresponder ao regex`() {
        val targetCbzName = "Chapter 01.cbz"
        val existingNames = setOf("Chapter 01.pdf")
        val chapterRegex = Regex("Chapter \\d+\\.cbz")

        assertTrue(validator.isPdfConversionEligible(targetCbzName, existingNames, chapterRegex))
    }

    @Test
    fun `isPdfConversionEligible deve retornar falso se o CBZ de destino ja existir`() {
        val targetCbzName = "Chapter 01.cbz"
        val existingNames = setOf("Chapter 01.cbz", "Chapter 01.pdf")
        val chapterRegex = Regex("Chapter \\d+\\.cbz")

        assertFalse(validator.isPdfConversionEligible(targetCbzName, existingNames, chapterRegex))
    }

    @Test
    fun `isPdfConversionEligible deve retornar falso se o CBZ de destino nao corresponder ao regex`() {
        val targetCbzName = "Random File.cbz"
        val existingNames = setOf("Random File.pdf")
        val chapterRegex = Regex("Chapter \\d+\\.cbz")

        assertFalse(validator.isPdfConversionEligible(targetCbzName, existingNames, chapterRegex))
    }

    @Test
    fun `isDuplicateSort deve retornar verdadeiro se o sort normalizado ja foi processado`() {
        val processedSorts = setOf("1", "2")
        val normalizedSort = "1"

        assertTrue(validator.isDuplicateSort(processedSorts, normalizedSort))
    }

    @Test
    fun `isDuplicateSort deve retornar falso se o sort normalizado nao foi processado`() {
        val processedSorts = setOf("1", "2")
        val normalizedSort = "3"

        assertFalse(validator.isDuplicateSort(processedSorts, normalizedSort))
    }
}
