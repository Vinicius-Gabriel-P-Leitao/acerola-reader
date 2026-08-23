package br.acerola.comic.local.mapper

import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.entity.metadata.relationship.TypeAuthor
import br.acerola.comic.local.translator.persistence.toEntity
import br.acerola.comic.local.translator.ui.toViewDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataMapperTest {
    @Test
    fun `MetadataRelations toViewDto should map full hierarchy`() {
        val comic = MetadataFixtures.createMangaRemoteInfo(title = "Berserk")
        val author = MetadataFixtures.createAuthor(name = "Kentaro Miura")
        val genre = MetadataFixtures.createGenre(genre = "Seinen")

        val relations =
            MetadataFixtures.createRemoteInfoRelations(
                remoteInfo = comic,
                authors = listOf(author),
                genres = listOf(genre),
            )

        val dto = relations.toViewDto()

        assertEquals("Berserk", dto.title)
        assertEquals("Kentaro Miura", dto.authors?.name)
        assertEquals(1, dto.genre.size)
        assertEquals("Seinen", dto.genre[0].name)
    }

    @Test
    fun `Author toViewDto and AuthorDto toEntity should be symmetric`() {
        val entity = MetadataFixtures.createAuthor(name = "Oda", type = TypeAuthor.AUTHOR)

        val dto = entity.toViewDto()
        assertEquals(entity.name, dto.name)
        assertEquals("author", dto.type)

        val backToModel = dto.toEntity(comicRemoteInfoFk = 99L)
        assertEquals(dto.name, backToModel.name)
        assertEquals(TypeAuthor.AUTHOR, backToModel.type)
        assertEquals(99L, backToModel.comicRemoteInfoFk)
    }

    @Test
    fun `MangaMetadataDto toEntity should map base fields`() {
        val dto = MetadataFixtures.createMangaRemoteInfoDto(title = "Test", year = 2024)

        val entity = dto.toEntity()

        assertEquals("Test", entity.title)
        assertEquals(2024, entity.publication)
    }
}
