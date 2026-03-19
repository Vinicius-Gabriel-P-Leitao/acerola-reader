package br.acerola.manga.service.archive

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.fixtures.LookupFixtures
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.relationship.CoverDao
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexFetchCoverRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.OutputStream

class MangaSaveCoverServiceTest {

    @MockK
    lateinit var context: Context
    @MockK
    lateinit var coverDao: CoverDao
    @MockK
    lateinit var directoryDao: MangaDirectoryDao
    @MockK
    lateinit var downloadCoverService: MangadexFetchCoverRepository

    private lateinit var service: MangaSaveCoverService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        service = MangaSaveCoverService(context, coverDao, directoryDao, downloadCoverService)
        mockkStatic(DocumentFile::class)
    }

    @After
    fun tearDown() = unmockkStatic(DocumentFile::class)

    @Test
    fun `processCover deve baixar imagem e atualizar base de dados com sucesso`() = runTest {
        // Arrange
        val rootUri = mockk<Uri>()
        val coverDto = LookupFixtures.createCoverDto()
        val mangaDir = MangaDirectoryFixtures.createMangaDirectory(id = 1, name = "One Piece")
        val bytes = byteArrayOf(0, 1, 2)

        val rootDoc = mockk<DocumentFile>()
        val mangaDoc = mockk<DocumentFile>()
        val newFileDoc = mockk<DocumentFile>()
        val fileUri = mockk<Uri>()

        every { DocumentFile.fromTreeUri(context, rootUri) } returns rootDoc
        every { rootDoc.exists() } returns true
        every { rootDoc.findFile("One Piece") } returns mangaDoc
        every { mangaDoc.canWrite() } returns true

        coEvery { downloadCoverService.searchCover(coverDto.url) } returns Either.Right(bytes)

        every { mangaDoc.findFile("cover.png") } returns null
        every { mangaDoc.createFile("image/png", "cover.png") } returns newFileDoc
        every { newFileDoc.uri } returns fileUri
        every { fileUri.toString() } returns "content://cover/1"

        val outputStream = mockk<OutputStream>(relaxed = true)
        val resolver = mockk<ContentResolver>()
        every { context.contentResolver } returns resolver
        every { resolver.openOutputStream(fileUri) } returns outputStream

        coEvery { directoryDao.getMangaDirectoryById(1) } returns mangaDir
        coEvery { directoryDao.update(any()) } returns Unit
        coEvery { coverDao.insert(any()) } returns 10L

        // Act
        val result = service.processCover(rootUri, 1, coverDto, "One Piece", 100)

        // Assert
        assertEquals(10L, result)
        coVerify { outputStream.write(bytes) }
        coVerify { directoryDao.update(match { it.cover == "content://cover/1" }) }
    }

    @Test
    fun `processCover deve apenas atualizar coverDao se download falhar`() = runTest {
        // Arrange
        val rootUri = mockk<Uri>()
        val coverDto = LookupFixtures.createCoverDto()

        every { DocumentFile.fromTreeUri(context, rootUri) } returns null // Falha ao acessar FS
        coEvery { coverDao.insert(any()) } returns 10L

        // Act
        val result = service.processCover(rootUri, 1, coverDto, "One Piece", 100)

        // Assert
        assertEquals(10L, result)
        coVerify(exactly = 0) { directoryDao.update(any()) }
    }
}
