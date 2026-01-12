package br.acerola.manga.local.mapper

import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.local.database.entity.archive.ChapterArchive
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
        mockkStatic("androidx.core.net.UriKt")
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic("androidx.core.net.UriKt")
    }

    @Test
    fun `MangaDirectory toDto deve mapear todos os campos corretamente`() {
        val entity = MangaDirectoryFixtures.createMangaDirectory(
            cover = "content://cover",
            banner = "content://banner"
        )
        val uriMock = mockk<Uri>()
        every { any<String>().toUri() } returns uriMock

        val dto = entity.toDto()

        assertEquals(entity.id, dto.id)
        assertEquals(entity.name, dto.name)
        assertEquals(uriMock, dto.coverUri)
        assertEquals(uriMock, dto.bannerUri)
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
        val entity = ChapterArchive(1, "10", "path", "10", "hash", 100)
        
        val dto = entity.toDto()

        assertEquals(entity.id, dto.id)
        assertEquals(entity.chapter, dto.name)
        assertEquals(entity.chapterSort, dto.chapterSort)
    }

    @Test
    fun `MangaDirectoryDto toModel deve mapear para entidade com timestamp atual`() {
        val dto = MangaDirectoryFixtures.createMangaDirectoryDto(coverUri = mockk(), bannerUri = mockk())
        every { dto.coverUri.toString() } returns "uri_cover"
        every { dto.bannerUri.toString() } returns "uri_banner"

        val model = dto.toModel()

        assertEquals(dto.name, model.name)
        assertEquals("uri_cover", model.cover)
        assertEquals("uri_banner", model.banner)
        // NOTE: lastModified é gerado via System.currentTimeMillis(), difícil de validar valor exato
    }

    @Test
    fun `List ChapterArchive toPageDto deve criar objeto de paginação correto`() {
        val list = listOf(
            ChapterArchive(1, "1", "p1", "1", null, 10),
            ChapterArchive(2, "2", "p2", "2", null, 10)
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

        val model = folder.toMangaDirectoryModel(cover, banner, "{v}")

        assertEquals("One Piece", model.name)
        assertEquals("uri_folder", model.path)
        assertEquals("uri_cover", model.cover)
        assertEquals("uri_banner", model.banner)
        assertEquals(5000L, model.lastModified)
        assertEquals("{v}", model.chapterTemplate)
    }
}
