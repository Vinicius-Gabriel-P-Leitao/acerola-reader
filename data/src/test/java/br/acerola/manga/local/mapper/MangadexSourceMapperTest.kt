package br.acerola.manga.local.mapper

import android.content.Context
import br.acerola.manga.data.R
import br.acerola.manga.local.translator.toDto
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterAttributes
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterPage
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import br.acerola.manga.remote.mangadex.dto.manga.MangaAttributes
import br.acerola.manga.remote.mangadex.dto.manga.MangaMangadexDto
import br.acerola.manga.remote.mangadex.dto.manga.Relationship
import br.acerola.manga.remote.mangadex.dto.manga.RelationshipAttributes
import br.acerola.manga.remote.mangadex.dto.manga.Tag
import br.acerola.manga.remote.mangadex.dto.manga.TagAttributes
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangadexSourceMapperTest {

    @Test
    fun `MangaMangadexDto toDto deve extrair autor e capa das relacoes`() {
        val context = mockk<Context>()
        every { context.getString(R.string.description_manga_untitled) } returns "Untitled"

        val attributes = MangaAttributes(
            titleMap = mapOf("en" to "Solo Leveling"),
            status = "ongoing",
            links = null,
            tags = listOf(
                Tag("t1", "tag", TagAttributes(mapOf("en" to "Action"), "group", 1))
            )
        )
        
        val relationships = listOf(
            Relationship("a1", "author", attributes = RelationshipAttributes(name = "Author Name")),
            Relationship("c1", "cover_art", attributes = RelationshipAttributes(fileName = "cover.jpg"))
        )

        val mangaRaw = MangaMangadexDto("m1", "manga", attributes, relationships)

        val dto = mangaRaw.toDto(context)

        assertEquals("Solo Leveling", dto.title)
        assertEquals("Author Name", dto.authors?.name)
        assertEquals("cover.jpg", dto.cover?.fileName)
        assertEquals(1, dto.genre.size)
        assertEquals("Action", dto.genre[0].name)
    }

    @Test
    fun `ChapterMangadexDto toDto deve construir URLs de paginas quando source fornecido`() {
        val attr = ChapterAttributes("1", "1", "Ch 1", pages = 20, version = 1)
        val chapterRaw = ChapterMangadexDto("ch1", "chapter", attr, emptyList())
        
        val source = ChapterSourceMangadexDto(
            baseUrl = "https://server.com",
            chapter = ChapterPage("hash123", data = listOf("1.jpg", "2.jpg"))
        )

        val dto = chapterRaw.toDto(source)

        assertEquals(2, dto.pageUrls.size)
        assertEquals("https://server.com/data/hash123/1.jpg", dto.pageUrls[0])
        assertEquals("https://server.com/data/hash123/2.jpg", dto.pageUrls[1])
    }

    @Test
    fun `ChapterMangadexDto toDto deve retornar lista vazia de URLs se source for nulo`() {
        val attr = ChapterAttributes("1", "1", "Ch 1", pages = 20, version = 1)
        val chapterRaw = ChapterMangadexDto("ch1", "chapter", attr, emptyList())

        val dto = chapterRaw.toDto(null)

        assertTrue(dto.pageUrls.isEmpty())
    }
}
