package br.acerola.comic.repository.adapter.local.comic

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import android.provider.DocumentsContract
import br.acerola.comic.config.preference.ComicDirectoryPreference
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.library.ComicDirectoryEngine
import br.acerola.comic.util.ContentQueryHelper
import br.acerola.comic.util.FastFileMetadata
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

import br.acerola.comic.service.template.TemplateMatcher
import br.acerola.comic.service.template.ChapterNameProcessor

@OptIn(ExperimentalCoroutinesApi::class)
class ComicDirectoryEngineTest {

    @MockK(relaxed = true) lateinit var context: Context
    @MockK lateinit var directoryDao: ComicDirectoryDao
    @MockK lateinit var mangaDirectoryOps: ChapterGateway<ChapterArchivePageDto>
    @MockK lateinit var templateService: ChapterNameProcessor
    @MockK lateinit var templateMatcher: TemplateMatcher

    private lateinit var repository: ComicDirectoryEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = ComicDirectoryEngine(directoryDao, templateMatcher, templateService, context)
        repository.mangaDirectoryOps = mangaDirectoryOps

        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
        mockkStatic(DocumentsContract::class)
        mockkObject(ContentQueryHelper)
        mockkObject(ComicDirectoryPreference)
        
        every { context.contentResolver } returns mockk(relaxed = true)
        every { context.applicationContext } returns context
        every { context.filesDir } returns mockk(relaxed = true)
        every { DocumentsContract.getTreeDocumentId(any()) } returns "root_id"
        every { DocumentsContract.buildChildDocumentsUriUsingTree(any(), any()) } returns mockk(relaxed = true)
        every { DocumentsContract.buildDocumentUriUsingTree(any(), any()) } returns mockk(relaxed = true)
        
        every { directoryDao.getAllMangaDirectory() } returns flowOf(emptyList())
        every { directoryDao.getAllMangaDirectoryIncludingHidden() } returns flowOf(emptyList())
        coEvery { mangaDirectoryOps.refreshComicChapters(any(), any()) } returns Either.Right(Unit)
        every { ContentQueryHelper.listFiles(any(), any(), any()) } returns Either.Right(emptyList())
        every { ContentQueryHelper.listFiles(any(), any()) } returns Either.Right(emptyList())
        every { ComicDirectoryPreference.folderUriFlow(any()) } returns flowOf("content://root")
        coEvery { templateService.getTemplates() } returns emptyList()
        every { templateMatcher.detect(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentFile::class)
        unmockkStatic(DocumentsContract::class)
        unmockkObject(ContentQueryHelper)
        unmockkObject(ComicDirectoryPreference)
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshManga deve atualizar metadados quando pasta foi modificada`() = runTest {
        val mangaId = 1L
        val existingManga = MangaDirectoryFixtures.createMangaDirectory(id = mangaId, lastModified = 1000L)
        val uriMock = mockk<Uri>()
        val folderDocMock = mockk<DocumentFile>()

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns existingManga
        every { Uri.parse(any()) } returns uriMock
        every { DocumentFile.fromSingleUri(context, uriMock) } returns folderDocMock

        every { folderDocMock.lastModified() } returns 90000000L
        every { folderDocMock.name } returns "Manga Folder"
        every { folderDocMock.uri } returns mockk(relaxed = true)
        every { folderDocMock.isDirectory } returns true
        
        val childMetadata = FastFileMetadata(id = "cap1", name = "cap1.cbz", size = 100, lastModified = 500, mimeType = "application/zip")
        every { ContentQueryHelper.listFiles(context, any(), any()) } returns Either.Right(listOf(childMetadata))

        coEvery { directoryDao.update(any()) } returns Unit

        val result = repository.refreshManga(mangaId)

        assertTrue("Esperado Right, mas foi $result", result.isRight())
        coVerify(exactly = 1) { directoryDao.update(any()) }
    }

    @Test
    fun `deve realizar scan recursivo e encontrar mangas em subpastas`() = runTest {
        val rootUri = mockk<Uri>()
        
        // Root tem uma subpasta "Hellboy"
        val subDirMetadata = FastFileMetadata(id = "hellboy_id", name = "Hellboy", size = 0, lastModified = 100, mimeType = DocumentsContract.Document.MIME_TYPE_DIR)
        every { ContentQueryHelper.listFiles(context, rootUri, "root_id") } returns Either.Right(listOf(subDirMetadata))
        
        // "Hellboy" tem uma subpasta "BPRD"
        val targetDirMetadata = FastFileMetadata(id = "bprd_id", name = "BPRD", size = 0, lastModified = 200, mimeType = DocumentsContract.Document.MIME_TYPE_DIR)
        every { ContentQueryHelper.listFiles(context, rootUri, "hellboy_id") } returns Either.Right(listOf(targetDirMetadata))
        
        // "BPRD" tem arquivos .cbz
        val mangaFileMetadata = FastFileMetadata(id = "manga_file", name = "issue1.cbz", size = 1000, lastModified = 300, mimeType = "application/zip")
        every { ContentQueryHelper.listFiles(context, rootUri, "bprd_id") } returns Either.Right(listOf(mangaFileMetadata))
        
        // Mock do DocumentFile para a pasta final
        val bprdUri = mockk<Uri>()
        val bprdDoc = mockk<DocumentFile>(relaxed = true) {
            every { name } returns "BPRD"
            every { uri } returns bprdUri
            every { lastModified() } returns 200L
        }
        every { DocumentsContract.buildDocumentUriUsingTree(rootUri, "bprd_id") } returns bprdUri
        every { DocumentFile.fromSingleUri(context, bprdUri) } returns bprdDoc
        
        coEvery { directoryDao.upsertMangaDirectoryTransaction(any(), any()) } returns 1L

        val result = repository.incrementalScan(rootUri)

        assertTrue(result.isRight())
        coVerify { directoryDao.upsertMangaDirectoryTransaction(match { it.name == "BPRD" }, any()) }
    }

    @Test
    fun `observeLibrary deve emitir lista de DTOs corretamente`() = runTest {
        val entityList = listOf(MangaDirectoryFixtures.createMangaDirectory(id = 1, name = "Manga A"))
        every { directoryDao.getAllMangaDirectoryIncludingHidden() } returns flowOf(entityList)
        every { Uri.parse(any()) } returns mockk(relaxed = true)

        val result = repository.observeLibrary().first { it.isNotEmpty() }

        assertEquals(1, result.size)
        assertEquals("Manga A", result[0].name)
    }
}
