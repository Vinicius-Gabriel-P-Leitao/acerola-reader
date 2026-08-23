package br.acerola.comic.pattern

import br.acerola.comic.pattern.media.MediaFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFileTest {
    @Test
    fun `isCover should identify cover files correctly`() {
        assertTrue(MediaFile.isCover("cover.jpg"))
        assertTrue(MediaFile.isCover("COVER.PNG"))
        assertTrue(MediaFile.isCover("folder.webp"))
        assertTrue(MediaFile.isCover("front.jpeg"))
        assertTrue(MediaFile.isCover("00_capa.jpg"))

        assertFalse(MediaFile.isCover("cover.txt"))
        assertFalse(MediaFile.isCover("chapter_01.jpg"))
        assertFalse(MediaFile.isCover(null))
        assertFalse(MediaFile.isCover(""))
    }

    @Test
    fun `isBanner should identify banner files correctly`() {
        assertTrue(MediaFile.isBanner("banner.jpg"))
        assertTrue(MediaFile.isBanner("BANNER.PNG"))
        assertTrue(MediaFile.isBanner("meu_banner_customizado.webp"))

        assertFalse(MediaFile.isBanner("banner.txt"))
        assertFalse(MediaFile.isBanner("cover.jpg"))
        assertFalse(MediaFile.isBanner(null))
    }

    @Test
    fun `isImage should validate image extensions`() {
        assertTrue(MediaFile.isImage("file.jpg"))
        assertTrue(MediaFile.isImage("file.jpeg"))
        assertTrue(MediaFile.isImage("file.png"))
        assertTrue(MediaFile.isImage("file.webp"))

        assertFalse(MediaFile.isImage("file.gif"))
        assertFalse(MediaFile.isImage("file.pdf"))
        assertFalse(MediaFile.isImage("file"))
        assertFalse(MediaFile.isImage(null))
    }

    @Test
    fun `matches should validate if name matches banner or cover enum`() {
        assertTrue(MediaFile.COVER.matches("cover.jpg"))
        assertTrue(MediaFile.BANNER.matches("banner.png"))

        assertFalse(MediaFile.COVER.matches("folder.jpg")) // from usa matches() estrito para o baseName
        assertFalse(MediaFile.COVER.matches("cover.txt"))
        assertFalse(MediaFile.COVER.matches(""))
    }
}
