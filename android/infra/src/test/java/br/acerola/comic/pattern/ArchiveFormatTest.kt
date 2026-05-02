package br.acerola.comic.pattern

import br.acerola.comic.pattern.archive.ArchiveFormat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveFormatTest {
    @Test
    fun isSupportedDeveIdentificarCbzCorretamente() {
        assertTrue(ArchiveFormat.isSupported("cbz"))
        assertTrue(ArchiveFormat.isSupported(".cbz"))
        assertTrue(ArchiveFormat.isSupported("file.cbz"))

        assertFalse(ArchiveFormat.isSupported("txt"))
        assertFalse(ArchiveFormat.isSupported(".zip"))
    }

    @Test
    fun isIndexableDeveRetornarTrueParaCbz() {
        assertTrue(ArchiveFormat.isIndexable("cbz"))
        assertTrue(ArchiveFormat.isIndexable("file.cbz"))

        // PDF is indexable = false in enum
        assertFalse(ArchiveFormat.isIndexable("pdf"))
    }
}
