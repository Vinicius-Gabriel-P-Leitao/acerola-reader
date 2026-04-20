package br.acerola.comic.pattern

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFilePatternTest {
    @Test
    fun `isCover deve identificar corretamente arquivos de capa`() {
        assertTrue(MediaFilePattern.isCover("cover.jpg"))
        assertTrue(MediaFilePattern.isCover("COVER.PNG"))
        assertTrue(MediaFilePattern.isCover("folder.webp"))
        assertTrue(MediaFilePattern.isCover("front.jpeg"))
        assertTrue(MediaFilePattern.isCover("00_capa.jpg"))

        assertFalse(MediaFilePattern.isCover("cover.txt"))
        assertFalse(MediaFilePattern.isCover("chapter_01.jpg"))
        assertFalse(MediaFilePattern.isCover(null))
        assertFalse(MediaFilePattern.isCover(""))
    }

    @Test
    fun `isBanner deve identificar corretamente arquivos de banner`() {
        assertTrue(MediaFilePattern.isBanner("banner.jpg"))
        assertTrue(MediaFilePattern.isBanner("BANNER.PNG"))
        assertTrue(MediaFilePattern.isBanner("meu_banner_customizado.webp"))

        assertFalse(MediaFilePattern.isBanner("banner.txt"))
        assertFalse(MediaFilePattern.isBanner("cover.jpg"))
        assertFalse(MediaFilePattern.isBanner(null))
    }

    @Test
    fun `isImage deve validar extensoes de imagem`() {
        assertTrue(MediaFilePattern.isImage("file.jpg"))
        assertTrue(MediaFilePattern.isImage("file.jpeg"))
        assertTrue(MediaFilePattern.isImage("file.png"))
        assertTrue(MediaFilePattern.isImage("file.webp"))

        assertFalse(MediaFilePattern.isImage("file.gif"))
        assertFalse(MediaFilePattern.isImage("file.pdf"))
        assertFalse(MediaFilePattern.isImage("file"))
        assertFalse(MediaFilePattern.isImage(null))
    }

    @Test
    fun `matches deve validar se o nome bate com o enum de banner ou cover`() {
        assertTrue(MediaFilePattern.COVER.matches("cover.jpg"))
        assertTrue(MediaFilePattern.BANNER.matches("banner.png"))

        assertFalse(MediaFilePattern.COVER.matches("folder.jpg")) // from usa matches() estrito para o baseName
        assertFalse(MediaFilePattern.COVER.matches("cover.txt"))
        assertFalse(MediaFilePattern.COVER.matches(""))
    }
}
