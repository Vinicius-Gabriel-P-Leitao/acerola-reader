package br.acerola.comic.local.mapper

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.translator.persistence.toEntity
import br.acerola.comic.local.translator.persistence.toMangaDirectoryEntity
import br.acerola.comic.local.translator.ui.toViewDto
import br.acerola.comic.local.translator.ui.toViewPageDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `MangaDirectory toViewDto deve mapear todos os campos corretamente`() {
        val entity =
            MangaDirectoryFixtures.createMangaDirectory(
                cover = "content://cover",
                banner = "content://banner",
            )
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock

        val dto = entity.toViewDto()

        assertEquals(entity.id, dto.id)
        assertEquals(entity.name, dto.name)
        assertEquals(uriMock, dto.coverUri)
        assertEquals(uriMock, dto.bannerUri)
    }

    @Test
    fun `MangaDirectory toViewDto deve tratar campos nulos`() {
        val entity = MangaDirectoryFixtures.createMangaDirectory(cover = null, banner = null)

        val dto = entity.toViewDto()

        assertNull(dto.coverUri)
        assertNull(dto.bannerUri)
    }

    @Test
    fun `ChapterArchive toViewDto deve mapear campos corretamente`() {
        val entity =
            ChapterArchive(
                id = 1,
                chapter = "10",
                path = "path",
                chapterSort = "10",
                checksum = "hash",
                fastHash = "hash",
                folderPathFk = 1,
                lastModified = 500L,
            )

        val dto = entity.toViewDto()

        assertEquals(entity.id, dto.id)
        assertEquals(entity.chapter, dto.name)
        assertEquals(entity.chapterSort, dto.chapterSort)
        assertEquals(500L, dto.lastModified)
    }

    @Test
    fun `MangaDirectoryDto toEntity deve mapear para entidade`() {
        val coverUri = mockk<Uri>()
        val bannerUri = mockk<Uri>()
        val dto =
            MangaDirectoryFixtures.createMangaDirectoryDto(
                coverUri = coverUri,
                bannerUri = bannerUri,
            )
        every { coverUri.toString() } returns "uri_cover"
        every { bannerUri.toString() } returns "uri_banner"

        val entity = dto.toEntity()

        assertEquals(dto.name, entity.name)
        assertEquals("uri_cover", entity.cover)
        assertEquals("uri_banner", entity.banner)
    }

    @Test
    fun `List ChapterArchive toViewPageDto deve criar objeto de paginação correto`() {
        val list =
            listOf(
                ChapterArchive(
                    id = 1,
                    chapter = "1",
                    path = "p1",
                    chapterSort = "1",
                    checksum = null,
                    fastHash = "10",
                    folderPathFk = 1,
                ),
                ChapterArchive(
                    id = 2,
                    chapter = "2",
                    path = "p2",
                    chapterSort = "2",
                    checksum = null,
                    fastHash = "10",
                    folderPathFk = 1,
                ),
            )

        val pageDto = list.toViewPageDto(pageSize = 10, total = 100, page = 1)

        assertEquals(2, pageDto.items.size)
        assertEquals(10, pageDto.pageSize)
        assertEquals(100, pageDto.total)
        assertEquals(1, pageDto.page)
    }

    @Test
    fun `DocumentFile toMangaDirectoryEntity deve mapear metadados do arquivo`() {
        val folder = mockk<DocumentFile>()
        val cover = mockk<DocumentFile>()
        val banner = mockk<DocumentFile>()

        val folderUri = mockk<Uri>()
        val coverUri = mockk<Uri>()
        val bannerUri = mockk<Uri>()

        every { folder.name } returns "One Piece"
        every { folder.uri } returns folderUri
        every { folderUri.toString() } returns "uri_folder"
        every { folder.lastModified() } returns 5000L

        every { cover.uri } returns coverUri
        every { coverUri.toString() } returns "uri_cover"

        every { banner.uri } returns bannerUri
        every { bannerUri.toString() } returns "uri_banner"

        val entity = folder.toMangaDirectoryEntity(cover, banner, chapterTemplateFk = null)

        assertEquals("One Piece", entity.name)
        assertEquals("uri_folder", entity.path)
        assertEquals("uri_cover", entity.cover)
        assertEquals("uri_banner", entity.banner)
        assertEquals(5000L, entity.lastModified)
    }
}
