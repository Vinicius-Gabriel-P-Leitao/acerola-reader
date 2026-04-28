package br.acerola.comic.local.mapper

import android.content.Context
import br.acerola.comic.data.R
import br.acerola.comic.local.translator.remote.toViewDto
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterAttributes
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterPage
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import br.acerola.comic.remote.mangadex.dto.manga.MangaAttributes
import br.acerola.comic.remote.mangadex.dto.manga.MangaMangadexDto
import br.acerola.comic.remote.mangadex.dto.manga.Relationship
import br.acerola.comic.remote.mangadex.dto.manga.RelationshipAttributes
import br.acerola.comic.remote.mangadex.dto.manga.Tag
import br.acerola.comic.remote.mangadex.dto.manga.TagAttributes
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangadexSourceMapperTest {
    @Test
    fun `MangaMangadexDto toViewDto deve extrair autor e capa das relacoes`() {
        val context = mockk<Context>()
        every { context.getString(R.string.description_manga_untitled) } returns "Untitled"

        val attributes =
            MangaAttributes(
                titleMap = mapOf("en" to "Solo Leveling"),
                status = "ongoing",
                links = null,
                tags =
                    listOf(
                        Tag("t1", "tag", TagAttributes(mapOf("en" to "Action"), "group", 1)),
                    ),
            )

        val relationships =
            listOf(
                Relationship("a1", "author", attributes = RelationshipAttributes(name = "Author Name")),
                Relationship("c1", "cover_art", attributes = RelationshipAttributes(fileName = "cover.jpg")),
            )

        val mangaRaw = MangaMangadexDto("m1", "comic", attributes, relationships)

        val dto = mangaRaw.toViewDto(context)

        assertEquals("Solo Leveling", dto.title)
        assertEquals("Author Name", dto.authors?.name)
        assertEquals("cover.jpg", dto.cover?.fileName)
        assertEquals(1, dto.genre.size)
        assertEquals("Action", dto.genre[0].name)
    }

    @Test
    fun `ChapterMangadexDto toViewDto deve construir URLs de paginas quando source fornecido`() {
        val attr = ChapterAttributes("1", "1", "Ch 1", pages = 20, version = 1)
        val chapterRaw = ChapterMangadexDto("ch1", "chapter", attr, emptyList())

        val source =
            ChapterSourceMangadexDto(
                baseUrl = "https://server.com",
                chapter = ChapterPage("hash123", data = listOf("1.jpg", "2.jpg")),
            )

        val dto = chapterRaw.toViewDto(source)

        assertEquals(2, dto.pageUrls.size)
        assertEquals("https://server.com/data/hash123/1.jpg", dto.pageUrls[0])
        assertEquals("https://server.com/data/hash123/2.jpg", dto.pageUrls[1])
    }

    @Test
    fun `ChapterMangadexDto toViewDto deve retornar lista vazia de URLs se source for nulo`() {
        val attr = ChapterAttributes("1", "1", "Ch 1", pages = 20, version = 1)
        val chapterRaw = ChapterMangadexDto("ch1", "chapter", attr, emptyList())

        val dto = chapterRaw.toViewDto(null)

        assertTrue(dto.pageUrls.isEmpty())
    }
}
