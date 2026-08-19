package br.acerola.comic.local.mapper

import android.content.Context
import br.acerola.comic.data.R
import br.acerola.comic.local.translator.remote.toViewDto
import br.acerola.comic.remote.mangadex.dto.comic.MangaAttributes
import br.acerola.comic.remote.mangadex.dto.comic.MangaMangadexDto
import br.acerola.comic.remote.mangadex.dto.comic.Relationship
import br.acerola.comic.remote.mangadex.dto.comic.RelationshipAttributes
import br.acerola.comic.remote.mangadex.dto.comic.Tag
import br.acerola.comic.remote.mangadex.dto.comic.TagAttributes
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MangadexSourceMapperTest {
    @Test
    fun `MangaMangadexDto toViewDto should extract author and cover from relationships`() {
        val context = mockk<Context>()
        every { context.getString(R.string.description_comic_untitled) } returns "Untitled"

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

        val comicRaw = MangaMangadexDto("m1", "comic", attributes, relationships)

        val dto = comicRaw.toViewDto(context)

        assertEquals("Solo Leveling", dto.title)
        assertEquals("Author Name", dto.authors?.name)
        assertEquals("cover.jpg", dto.cover?.fileName)
        assertEquals(1, dto.genre.size)
        assertEquals("Action", dto.genre[0].name)
    }
}
