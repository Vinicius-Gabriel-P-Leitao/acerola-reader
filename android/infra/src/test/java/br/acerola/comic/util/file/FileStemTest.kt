package br.acerola.comic.util.file

import org.junit.Assert.assertEquals
import org.junit.Test

class FileStemTest {
    @Test
    fun `fileStem remove apenas a ultima extensao`() {
        assertEquals("Ch. 1", "Ch. 1.cbz".fileStem())
        assertEquals("archive.tar", "archive.tar.gz".fileStem())
    }

    @Test
    fun `fileStem preserva dotfiles sem extensao real`() {
        assertEquals(".gitignore", ".gitignore".fileStem())
    }

    @Test
    fun `fileStem preserva nomes sem extensao`() {
        assertEquals("sem_extensao", "sem_extensao".fileStem())
    }

    @Test
    fun `fileStem remove a ultima extensao mesmo em dotfiles com extensao adicional`() {
        assertEquals(".gitignore", ".gitignore.bak".fileStem())
    }
}
