package br.acerola.comic.util.file

import org.junit.Assert.assertEquals
import org.junit.Test

class FileStemTest {
    @Test
    fun `fileStem removes only the last extension`() {
        assertEquals("Ch. 1", "Ch. 1.cbz".fileStem())
        assertEquals("archive.tar", "archive.tar.gz".fileStem())
    }

    @Test
    fun `fileStem preserves dotfiles without real extension`() {
        assertEquals(".gitignore", ".gitignore".fileStem())
    }

    @Test
    fun `fileStem preserves names without extension`() {
        assertEquals("sem_extensao", "sem_extensao".fileStem())
    }

    @Test
    fun `fileStem removes the last extension even on dotfiles with additional extension`() {
        assertEquals(".gitignore", ".gitignore.bak".fileStem())
    }
}
