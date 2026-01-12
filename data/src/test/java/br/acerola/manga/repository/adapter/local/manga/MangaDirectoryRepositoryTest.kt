package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.repository.port.ChapterManagementRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MangaDirectoryRepositoryTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var directoryDao: MangaDirectoryDao

    @MockK
    lateinit var archiveDao: ChapterArchiveDao

    @MockK
    lateinit var mangaDirectoryOps: ChapterManagementRepository<ChapterArchivePageDto>

    private lateinit var repository: MangaDirectoryRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = MangaDirectoryRepository(
            context = context,
            directoryDao = directoryDao,
            archiveDao = archiveDao
        )
        repository.mangaDirectoryOps = mangaDirectoryOps

        mockkStatic(Uri::class)
        mockkStatic("androidx.core.net.UriKt")
        mockkStatic(DocumentFile::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic("androidx.core.net.UriKt")
        unmockkStatic(DocumentFile::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshManga deve atualizar metadados quando pasta foi modificada`() = runTest {
        // Arrange
        val mangaId = 1L

        val existingManga = MangaDirectoryFixtures.createMangaDirectory(id = mangaId, lastModified = 1000L)
        val uriMock = mockk<Uri>()
        val folderDocMock = mockk<DocumentFile>()
        val coverFileMock = mockk<DocumentFile>(relaxed = true)

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns existingManga
        every { Uri.parse(any()) } returns uriMock
        every { DocumentFile.fromTreeUri(context, any()) } returns folderDocMock

        val docUri = mockk<Uri>()
        every { docUri.toString() } returns "content://uri"

        every { folderDocMock.lastModified() } returns 90000000L
        every { folderDocMock.name } returns "Manga Folder"
        every { folderDocMock.uri } returns docUri
        every { folderDocMock.isDirectory } returns true

        // WARN: Tem que tomar cuidado com esse mock, já que o URI usado no android é provido pela JBR e não jvm
        every { coverFileMock.name } returns "cover.jpg"
        every { coverFileMock.uri } returns mockk()
        every { coverFileMock.isFile } returns true

        every { folderDocMock.listFiles() } returns arrayOf(coverFileMock)

        coEvery { directoryDao.update(any()) } returns Unit

        // Act
        val result = repository.refreshManga(mangaId)

        // Assert
        assertTrue("Esperado Right, mas foi $result", result.isRight())

        verify { DocumentFile.fromTreeUri(any(), any()) }
        coVerify(exactly = 1) { directoryDao.update(any()) }
    }

    @Test
    fun `deve retornar DatabaseError em caso de falha de persistencia no refreshManga`() = runTest {
        val mangaId = 1L
        coEvery { directoryDao.getMangaDirectoryById(mangaId) } throws SQLiteException("DB Error")

        val result = repository.refreshManga(mangaId)

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is LibrarySyncError.DatabaseError) }
    }

    @Test
    fun `deve realizar scan incremental adicionando e removendo pastas`() = runTest {
        val rootUri = mockk<Uri>()
        val rootDoc = mockk<DocumentFile>()
        val existingManga = MangaDirectoryFixtures.createMangaDirectory(name = "Old", path = "uri/old")

        val newDoc = mockk<DocumentFile>()
        val newUri = mockk<Uri>()

        every { newUri.toString() } returns "uri/new"
        every { newDoc.isDirectory } returns true
        every { newDoc.name } returns "New"
        every { newDoc.uri } returns newUri
        every { newDoc.lastModified() } returns 100L
        every { newDoc.listFiles() } returns emptyArray()

        every { DocumentFile.fromTreeUri(context, rootUri) } returns rootDoc
        every { rootDoc.listFiles() } returns arrayOf(newDoc)
        coEvery { directoryDao.getAllMangaDirectory() } returns flowOf(listOf(existingManga))
        coEvery { directoryDao.delete(existingManga) } returns Unit
        coEvery { directoryDao.insert(any()) } returns 1L

        val result = repository.incrementalScan(rootUri)

        assertTrue(result.isRight())
        coVerify { directoryDao.delete(existingManga) }
        coVerify { directoryDao.insert(match { it.name == "New" }) }
    }

    @Test
    fun `deve retornar FolderAccessDenied quando nao houver permissao de acesso`() = runTest {
        val rootUri = mockk<Uri>()
        every { DocumentFile.fromTreeUri(context, rootUri) } throws SecurityException("No access")

        val result = repository.incrementalScan(rootUri)

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is LibrarySyncError.FolderAccessDenied) }
    }

    @Test
    fun `deve retornar DiskIOFailure quando ocorrer IOException`() = runTest {
        val rootUri = mockk<Uri>()
        every { rootUri.toString() } returns "content://root"
        every { DocumentFile.fromTreeUri(context, rootUri) } throws IOException("IO Failure")

        val result = repository.refreshLibrary(rootUri)

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is LibrarySyncError.DiskIOFailure) }
    }

    @Test
    fun `observeLibrary deve emitir lista de DTOs corretamente`() = runTest {
        // Arrange
        val entityList = listOf(
            MangaDirectoryFixtures.createMangaDirectory(id = 1, name = "Manga A"),
            MangaDirectoryFixtures.createMangaDirectory(id = 2, name = "Manga B")
        )

        every { directoryDao.getAllMangaDirectory() } returns flowOf(value = entityList)
        every { Uri.parse(any()) } returns mockk()

        // Act
        val result = repository.observeLibrary().first { it.isNotEmpty() }

        // Assert
        assertEquals(2, result.size)
        assertEquals("Manga A", result[0].name)
    }

    @Test
    fun `rebuildLibrary deve disparar refresh de capitulos para cada manga`() = runTest {
        val rootUri = mockk<Uri>()
        val rootDoc = mockk<DocumentFile>()
        val manga = MangaDirectoryFixtures.createMangaDirectory(id = 10L)

        every { DocumentFile.fromTreeUri(context, rootUri) } returns rootDoc
        every { rootDoc.listFiles() } returns emptyArray()
        coEvery { directoryDao.getAllMangaDirectory() } returns flowOf(listOf(manga))
        coEvery { mangaDirectoryOps.refreshMangaChapters(10L) } returns Either.Right(Unit)

        val result = repository.rebuildLibrary(rootUri)

        assertTrue(result.isRight())
        coVerify { mangaDirectoryOps.refreshMangaChapters(10L) }
    }

    @Test
    fun `incrementalScan deve retornar Unit se baseUri for nulo`() = runTest {
        val result = repository.incrementalScan(null)
        assertTrue(result.isRight())
    }
}
