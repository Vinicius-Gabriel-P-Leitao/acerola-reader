package br.acerola.comic.service.archive

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.right
import br.acerola.comic.error.message.IoError
import br.acerola.comic.fixtures.LookupFixtures
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.relationship.CoverDao
import br.acerola.comic.service.artwork.CoverSaver
import br.acerola.comic.service.file.FileStorageHandler
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoverSaverTest {
    @MockK lateinit var context: Context

    @MockK lateinit var coverDao: CoverDao

    @MockK lateinit var directoryDao: ComicDirectoryDao

    @MockK lateinit var fileStorageHandler: FileStorageHandler

    private lateinit var service: CoverSaver

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        service = CoverSaver(coverDao, directoryDao, fileStorageHandler, context)
        mockkStatic(DocumentFile::class)
    }

    @After
    fun tearDown() = unmockkStatic(DocumentFile::class)

    @Test
    fun `processCover deve salvar imagem no disco e atualizar base de dados com sucesso`() =
        runTest {
            // Arrange
            val rootUri = mockk<Uri>()
            val coverDto = LookupFixtures.createCoverDto()
            val bytes = byteArrayOf(0, 1, 2)
            val comicDir = MangaDirectoryFixtures.createMangaDirectory(id = 1, name = "One Piece")

            val comicDoc = mockk<DocumentFile>()
            val savedFileDoc = mockk<DocumentFile>()
            val fileUri = mockk<Uri>()
            val dirUri = mockk<Uri>()

            every { DocumentFile.fromTreeUri(context, any()) } returns comicDoc
            every { comicDoc.isDirectory } returns true
            every { comicDoc.uri } returns dirUri
            every { comicDoc.listFiles() } returns emptyArray()
            every { comicDoc.findFile(any()) } returns savedFileDoc
            every { savedFileDoc.uri } returns fileUri
            every { fileUri.toString() } returns "content://cover/1"

            coEvery { fileStorageHandler.saveFile(any(), any(), any(), any()) } returns "content://cover/1".right()
            coEvery { directoryDao.getDirectoryById(1) } returns comicDir
            coEvery { directoryDao.update(any()) } returns Unit
            coEvery { coverDao.insert(any()) } returns 10L

            // Act
            val result = service.processCover(rootUri, 1, bytes, coverDto.url, "One Piece", 100)

            // Assert
            assertTrue(result.isRight())
            result.onRight { assertEquals(10L, it) }
            coVerify { fileStorageHandler.saveFile(comicDoc, "cover.jpg", "image/jpeg", bytes) }
            coVerify { directoryDao.update(match { it.cover == "content://cover/1" }) }
        }

    @Test
    fun `processCover deve retornar FileNotFound se nao conseguir acessar FS`() =
        runTest {
            // Arrange
            val rootUri = mockk<Uri>()
            val bytes = byteArrayOf(0, 1, 2)
            val coverUrl = "https://mangadex.org/covers/1/a.jpg"
            val comicDir = MangaDirectoryFixtures.createMangaDirectory(id = 1, name = "One Piece")

            coEvery { directoryDao.getDirectoryById(1) } returns comicDir
            every { DocumentFile.fromTreeUri(context, any()) } returns null

            // Act
            val result = service.processCover(rootUri, 1, bytes, coverUrl, "One Piece", 100)

            // Assert
            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is IoError.FileNotFound) }
        }
}
