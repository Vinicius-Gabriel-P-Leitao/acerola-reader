package br.acerola.manga.local.mapper

import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.entity.metadata.relationship.TypeAuthor
import br.acerola.manga.local.translator.toDto
import br.acerola.manga.local.translator.toModel
import br.acerola.manga.local.translator.toPageDto
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteInfoMapperTest {

    @Test
    fun `RemoteInfoRelations toDto deve mapear hierarquia completa`() {
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

        val dto = relations.toDto()

        assertEquals("Berserk", dto.title)
        assertEquals("Kentaro Miura", dto.authors?.name)
        assertEquals(1, dto.genre.size)
        assertEquals("Seinen", dto.genre[0].name)
        assertEquals("url_test", dto.cover?.url)
    }

    @Test
    fun `Author toDto e AuthorDto toModel devem ser simétricos`() {
        val entity = MetadataFixtures.createAuthor(name = "Oda", type = TypeAuthor.AUTHOR)

        val dto = entity.toDto()
        assertEquals(entity.name, dto.name)
        assertEquals("author", dto.type)

        val backToModel = dto.toModel(mangaId = 99)
        assertEquals(dto.name, backToModel.name)
        assertEquals(TypeAuthor.AUTHOR, backToModel.type)
        assertEquals(99L, backToModel.mangaRemoteInfoFk)
    }

    @Test
    fun `ChapterRemoteInfo toDto deve ordenar sources por numero de pagina`() {
        val chapter = MetadataFixtures.createChapterRemoteInfo()
        val sources = listOf(
            MetadataFixtures.createChapterDownloadSource(pageNumber = 2, imageUrl = "img2"),
            MetadataFixtures.createChapterDownloadSource(pageNumber = 1, imageUrl = "img1")
        )

        val dto = chapter.toDto(sources)

        assertEquals(2, dto.source.size)
        assertEquals(1, dto.source[0].pageNumber)
        assertEquals(2, dto.source[1].pageNumber)
    }

    @Test
    fun `MangaRemoteInfoDto toModel deve mapear campos base`() {
        val dto = MetadataFixtures.createMangaRemoteInfoDto(title = "Test", year = 2024)
        
        val model = dto.toModel()

        assertEquals("Test", model.title)
        assertEquals(2024, model.publication)
    }

    @Test
    fun `List ChapterRemoteInfo toPageDto deve filtrar sources pelo FK do capitulo`() {
        val chapters = listOf(
            MetadataFixtures.createChapterRemoteInfo(id = 1),
            MetadataFixtures.createChapterRemoteInfo(id = 2)
        )
        val sources = listOf(
            MetadataFixtures.createChapterDownloadSource(chapterFk = 1, imageUrl = "s1"),
            MetadataFixtures.createChapterDownloadSource(chapterFk = 2, imageUrl = "s2"),
            MetadataFixtures.createChapterDownloadSource(chapterFk = 1, imageUrl = "s1-2")
        )

        val page = chapters.toPageDto(sources = sources)

        assertEquals(2, page.items.size)
        assertEquals(2, page.items.find { it.id == 1L }?.source?.size)
        assertEquals(1, page.items.find { it.id == 2L }?.source?.size)
    }
}
