package br.acerola.comic.pattern

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveFormatPatternTest {

    @Test
    fun isSupportedDeveIdentificarCbzCorretamente() {
        assertTrue(ArchiveFormatPattern.isSupported("cbz"))
        assertTrue(ArchiveFormatPattern.isSupported(".cbz"))
        assertTrue(ArchiveFormatPattern.isSupported("file.cbz"))
        
        assertFalse(ArchiveFormatPattern.isSupported("txt"))
        assertFalse(ArchiveFormatPattern.isSupported(".zip"))
    }

    @Test
    fun isIndexableDeveRetornarTrueParaCbz() {
        assertTrue(ArchiveFormatPattern.isIndexable("cbz"))
        assertTrue(ArchiveFormatPattern.isIndexable("file.cbz"))
        
        // PDF is indexable = false in enum
        assertFalse(ArchiveFormatPattern.isIndexable("pdf"))
    }
}
