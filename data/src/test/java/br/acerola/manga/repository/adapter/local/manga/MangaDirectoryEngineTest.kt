package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import android.provider.DocumentsContract
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.adapter.contract.ChapterPort
import br.acerola.manga.adapter.library.MangaDirectoryEngine
import br.acerola.manga.util.ContentQueryHelper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
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
class MangaDirectoryEngineTest {

    @MockK(relaxed = true) lateinit var context: Context
    @MockK lateinit var directoryDao: MangaDirectoryDao
    @MockK lateinit var mangaDirectoryOps: ChapterPort<ChapterArchivePageDto>

    private lateinit var repository: MangaDirectoryEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = MangaDirectoryEngine(context, directoryDao)
        repository.mangaDirectoryOps = mangaDirectoryOps

        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
        mockkStatic(DocumentsContract::class)
        mockkObject(ContentQueryHelper)
        mockkObject(MangaDirectoryPreference)
        
        every { context.contentResolver } returns mockk(relaxed = true)
        every { context.applicationContext } returns context
        every { context.filesDir } returns mockk(relaxed = true)
        every { DocumentsContract.getTreeDocumentId(any()) } returns "root_id"
        every { DocumentsContract.buildChildDocumentsUriUsingTree(any(), any()) } returns mockk(relaxed = true)
        every { DocumentsContract.buildDocumentUriUsingTree(any(), any()) } returns mockk(relaxed = true)
        
        // Mock padrão para evitar 'no answer found'
        every { directoryDao.getAllMangaDirectory() } returns flowOf(emptyList())
        coEvery { mangaDirectoryOps.refreshMangaChapters(any(), any()) } returns Either.Right(Unit)
        every { ContentQueryHelper.listFiles(any(), any(), any()) } returns Either.Right(emptyList())
        every { ContentQueryHelper.listFiles(any(), any()) } returns Either.Right(emptyList())
        every { MangaDirectoryPreference.folderUriFlow(any()) } returns flowOf("content://root")
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentFile::class)
        unmockkStatic(DocumentsContract::class)
        unmockkObject(ContentQueryHelper)
        unmockkObject(MangaDirectoryPreference)
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshManga deve atualizar metadados quando pasta foi modificada`() = runTest {
        val mangaId = 1L
        val existingManga = MangaDirectoryFixtures.createMangaDirectory(id = mangaId, lastModified = 1000L)
        val uriMock = mockk<Uri>()
        val folderDocMock = mockk<DocumentFile>()
        val coverFileMock = mockk<DocumentFile>(relaxed = true)

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns existingManga
        every { Uri.parse(any()) } returns uriMock
        every { DocumentFile.fromSingleUri(context, uriMock) } returns folderDocMock

        every { folderDocMock.lastModified() } returns 90000000L
        every { folderDocMock.name } returns "Manga Folder"
        every { folderDocMock.uri } returns mockk(relaxed = true)
        every { folderDocMock.isDirectory } returns true
        every { folderDocMock.findFile(any()) } returns null
        every { folderDocMock.listFiles() } returns arrayOf(coverFileMock)

        every { coverFileMock.name } returns "cover.jpg"
        every { coverFileMock.isFile } returns true
        every { coverFileMock.isDirectory } returns false

        coEvery { directoryDao.update(any()) } returns Unit

        val result = repository.refreshManga(mangaId)

        assertTrue("Esperado Right, mas foi $result", result.isRight())
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

        every { Uri.parse(any()) } returns rootUri
        every { newUri.toString() } returns "uri/new"
        every { newDoc.isDirectory } returns true
        every { newDoc.name } returns "New"
        every { newDoc.uri } returns newUri
        every { newDoc.lastModified() } returns 100L
        every { newDoc.listFiles() } returns emptyArray()
        every { newDoc.findFile(any()) } returns null

        every { DocumentFile.fromTreeUri(context, rootUri) } returns rootDoc
        
        // Mock do ContentQueryHelper para o buildLibrary
        val newFolderMetadata = br.acerola.manga.util.FastFileMetadata(
            id = "new_id",
            name = "New",
            size = 0L,
            lastModified = 100L,
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR
        )
        every { ContentQueryHelper.listFiles(context, rootUri) } returns Either.Right(listOf(newFolderMetadata))
        every { ContentQueryHelper.listFiles(context, rootUri, "new_id") } returns Either.Right(emptyList())
        
        every { DocumentsContract.buildDocumentUriUsingTree(rootUri, "new_id") } returns newUri
        every { DocumentFile.fromSingleUri(context, newUri) } returns newDoc
        
        every { directoryDao.getAllMangaDirectory() } returns flowOf(listOf(existingManga))
        coEvery { directoryDao.delete(existingManga) } returns Unit
        coEvery { directoryDao.insert(any()) } returns 1L

        val result = repository.incrementalScan(rootUri)

        assertTrue("Deveria ser sucesso mas foi: $result", result.isRight())
        coVerify { directoryDao.delete(existingManga) }
        coVerify { directoryDao.insert(match { it.name == "New" }) }
    }

    @Test
    fun `deve retornar sucesso mesmo que pasta seja inacessivel no scan`() = runTest {
        val rootUri = mockk<Uri>()
        every { Uri.parse(any()) } returns rootUri
        every { DocumentFile.fromTreeUri(context, rootUri) } returns null

        val result = repository.incrementalScan(rootUri)

        assertTrue("Deveria ser sucesso (pasta nula apenas para o scan)", result.isRight())
    }

    @Test
    fun `deve retornar DiskIOFailure quando ocorrer IOException`() = runTest {
        val rootUri = mockk<Uri>()
        every { Uri.parse(any()) } returns rootUri
        every { DocumentFile.fromTreeUri(context, rootUri) } throws IOException("IO Failure")

        val result = repository.refreshLibrary(rootUri)

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is LibrarySyncError.DiskIOFailure) }
    }

    @Test
    fun `observeLibrary deve emitir lista de DTOs corretamente`() = runTest {
        val entityList = listOf(MangaDirectoryFixtures.createMangaDirectory(id = 1, name = "Manga A"))
        every { directoryDao.getAllMangaDirectory() } returns flowOf(entityList)
        every { Uri.parse(any()) } returns mockk(relaxed = true)

        val result = repository.observeLibrary().first { it.isNotEmpty() }

        assertEquals(1, result.size)
        assertEquals("Manga A", result[0].name)
    }
}
