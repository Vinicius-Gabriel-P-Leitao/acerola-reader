package br.acerola.manga.local.mapper

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.translator.toDto
import br.acerola.manga.local.translator.toMangaDirectoryModel
import br.acerola.manga.local.translator.toModel
import br.acerola.manga.local.translator.toPageDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArchiveMapperTest {

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `MangaDirectory toDto deve mapear todos os campos corretamente`() {
        val entity = MangaDirectoryFixtures.createMangaDirectory(
            cover = "content://cover",
            banner = "content://banner"
        )
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock

        val dto = entity.toDto()

        assertEquals(entity.id, dto.id)
        assertEquals(entity.name, dto.name)
        assertEquals(uriMock, dto.coverUri)
        assertEquals(uriMock, dto.bannerUri)
    }

    @Test
    fun `MangaDirectory toDto deve mapear hasComicInfo corretamente`() {
        val entity = MangaDirectoryFixtures.createMangaDirectory(hasComicInfo = true)
        
        val dto = entity.toDto()

        assertTrue("hasComicInfo deveria ser true", dto.hasComicInfo)
    }

    @Test
    fun `MangaDirectory toDto deve tratar campos nulos`() {
        val entity = MangaDirectoryFixtures.createMangaDirectory(cover = null, banner = null)
        
        val dto = entity.toDto()

        assertNull(dto.coverUri)
        assertNull(dto.bannerUri)
    }

    @Test
    fun `ChapterArchive toDto deve mapear campos corretamente`() {
        val entity = ChapterArchive(
            id = 1,
            chapter = "10",
            path = "path",
            chapterSort = "10",
            checksum = "hash",
            fastHash = "hash",
            folderPathFk = 1
        )
        
        val dto = entity.toDto()

        assertEquals(entity.id, dto.id)
        assertEquals(entity.chapter, dto.name)
        assertEquals(entity.chapterSort, dto.chapterSort)
    }

    @Test
    fun `MangaDirectoryDto toModel deve mapear para entidade com timestamp atual`() {
        val coverUri = mockk<Uri>()
        val bannerUri = mockk<Uri>()
        val dto = MangaDirectoryFixtures.createMangaDirectoryDto(
            coverUri = coverUri, 
            bannerUri = bannerUri,
            hasComicInfo = true
        )
        every { coverUri.toString() } returns "uri_cover"
        every { bannerUri.toString() } returns "uri_banner"

        val model = dto.toModel()

        assertEquals(dto.name, model.name)
        assertEquals("uri_cover", model.cover)
        assertEquals("uri_banner", model.banner)
        assertTrue(model.hasComicInfo)
    }

    @Test
    fun `List ChapterArchive toPageDto deve criar objeto de paginação correto`() {
        val list = listOf(
            ChapterArchive(
                id = 1,
                chapter = "1",
                path = "p1",
                chapterSort = "1",
                checksum = null,
                fastHash = "10",
                folderPathFk = 1
            ),
            ChapterArchive(
                id = 2,
                chapter = "2",
                path = "p2",
                chapterSort = "2",
                checksum = null,
                fastHash = "10",
                folderPathFk = 1
            )
        )

        val pageDto = list.toPageDto(pageSize = 10, total = 100, page = 1)

        assertEquals(2, pageDto.items.size)
        assertEquals(10, pageDto.pageSize)
        assertEquals(100, pageDto.total)
        assertEquals(1, pageDto.page)
    }

    @Test
    fun `DocumentFile toMangaDirectoryModel deve mapear metadados do arquivo`() {
        val folder = mockk<DocumentFile>()
        val cover = mockk<DocumentFile>()
        val banner = mockk<DocumentFile>()
        
        every { folder.name } returns "One Piece"
        every { folder.uri.toString() } returns "uri_folder"
        every { folder.lastModified() } returns 5000L
        every { cover.uri.toString() } returns "uri_cover"
        every { banner.uri.toString() } returns "uri_banner"

        val model = folder.toMangaDirectoryModel(cover, banner, "{v}", hasComicInfo = true)

        assertEquals("One Piece", model.name)
        assertEquals("uri_folder", model.path)
        assertEquals("uri_cover", model.cover)
        assertEquals("uri_banner", model.banner)
        assertEquals(5000L, model.lastModified)
        assertEquals("{v}", model.chapterTemplate)
        assertTrue(model.hasComicInfo)
    }
}
