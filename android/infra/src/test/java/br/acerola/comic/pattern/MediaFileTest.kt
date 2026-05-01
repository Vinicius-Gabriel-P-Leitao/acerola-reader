package br.acerola.comic.pattern

import br.acerola.comic.pattern.media.MediaFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFileTest {
    @Test
    fun `isCover deve identificar corretamente arquivos de capa`() {
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
    fun `isBanner deve identificar corretamente arquivos de banner`() {
        assertTrue(MediaFile.isBanner("banner.jpg"))
        assertTrue(MediaFile.isBanner("BANNER.PNG"))
        assertTrue(MediaFile.isBanner("meu_banner_customizado.webp"))

        assertFalse(MediaFile.isBanner("banner.txt"))
        assertFalse(MediaFile.isBanner("cover.jpg"))
        assertFalse(MediaFile.isBanner(null))
    }

    @Test
    fun `isImage deve validar extensoes de imagem`() {
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
    fun `matches deve validar se o nome bate com o enum de banner ou cover`() {
        assertTrue(MediaFile.COVER.matches("cover.jpg"))
        assertTrue(MediaFile.BANNER.matches("banner.png"))

        assertFalse(MediaFile.COVER.matches("folder.jpg")) // from usa matches() estrito para o baseName
        assertFalse(MediaFile.COVER.matches("cover.txt"))
        assertFalse(MediaFile.COVER.matches(""))
    }
}
