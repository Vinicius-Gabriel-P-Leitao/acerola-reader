package br.acerola.comic.local.mapper

import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.entity.metadata.relationship.TypeAuthor
import br.acerola.comic.local.translator.persistence.toEntity
import br.acerola.comic.local.translator.ui.toViewDto
import br.acerola.comic.local.translator.ui.toViewPageDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataMapperTest {

    @Test
    fun `MetadataRelations toViewDto deve mapear hierarquia completa`() {
        val manga = MetadataFixtures.createMangaRemoteInfo(title = "Berserk")
        val author = MetadataFixtures.createAuthor(name = "Kentaro Miura")
        val genre = MetadataFixtures.createGenre(genre = "Seinen")
        val cover = MetadataFixtures.createCover(url = "url_test")

        val relations = MetadataFixtures.createRemoteInfoRelations(
            remoteInfo = manga,
            authors = listOf(author),
            genres = listOf(genre),
            covers = listOf(cover)
        )

        val dto = relations.toViewDto()

        assertEquals("Berserk", dto.title)
        assertEquals("Kentaro Miura", dto.authors?.name)
        assertEquals(1, dto.genre.size)
        assertEquals("Seinen", dto.genre[0].name)
        assertEquals("url_test", dto.cover?.url)
    }

    @Test
    fun `Author toViewDto e AuthorDto toEntity devem ser simétricos`() {
        val entity = MetadataFixtures.createAuthor(name = "Oda", type = TypeAuthor.AUTHOR)

        val dto = entity.toViewDto()
        assertEquals(entity.name, dto.name)
        assertEquals("author", dto.type)

        val backToModel = dto.toEntity(mangaId = 99)
        assertEquals(dto.name, backToModel.name)
        assertEquals(TypeAuthor.AUTHOR, backToModel.type)
        assertEquals(99L, backToModel.mangaRemoteInfoFk)
    }

    @Test
    fun `ChapterMetadata toViewDto deve ordenar sources por numero de pagina`() {
        val chapter = MetadataFixtures.createChapterRemoteInfo()
        val sources = listOf(
            MetadataFixtures.createChapterDownloadSource(pageNumber = 2, imageUrl = "img2"),
            MetadataFixtures.createChapterDownloadSource(pageNumber = 1, imageUrl = "img1")
        )

        val dto = chapter.toViewDto(sources)

        assertEquals(2, dto.source.size)
        assertEquals(1, dto.source[0].pageNumber)
        assertEquals(2, dto.source[1].pageNumber)
    }

    @Test
    fun `MangaMetadataDto toEntity deve mapear campos base`() {
        val dto = MetadataFixtures.createMangaRemoteInfoDto(title = "Test", year = 2024)
        
        val entity = dto.toEntity()

        assertEquals("Test", entity.title)
        assertEquals(2024, entity.publication)
    }

    @Test
    fun `List ChapterMetadata toViewPageDto deve filtrar sources pelo FK do capitulo`() {
        val chapters = listOf(
            MetadataFixtures.createChapterRemoteInfo(id = 1),
            MetadataFixtures.createChapterRemoteInfo(id = 2)
        )
        val sources = listOf(
            MetadataFixtures.createChapterDownloadSource(chapterFk = 1, imageUrl = "s1"),
            MetadataFixtures.createChapterDownloadSource(chapterFk = 2, imageUrl = "s2"),
            MetadataFixtures.createChapterDownloadSource(chapterFk = 1, imageUrl = "s1-2")
        )

        val page = chapters.toViewPageDto(sources = sources)

        assertEquals(2, page.items.size)
        assertEquals(2, page.items.find { it.id == 1L }?.source?.size)
        assertEquals(1, page.items.find { it.id == 2L }?.source?.size)
    }
}
