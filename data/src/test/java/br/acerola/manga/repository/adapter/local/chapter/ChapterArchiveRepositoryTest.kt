package br.acerola.manga.repository.adapter.local.chapter

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.util.ContentQueryHelper
import br.acerola.manga.util.FastFileMetadata
import br.acerola.manga.util.sha256
import br.acerola.manga.util.templateToRegex
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
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
class ChapterArchiveRepositoryTest {

    @MockK
    lateinit var directoryDao: MangaDirectoryDao

    @MockK
    lateinit var chapterArchiveDao: ChapterArchiveDao

    @MockK
    lateinit var context: Context

    private lateinit var repository: ChapterArchiveRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = ChapterArchiveRepository(directoryDao, chapterArchiveDao, context)

        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
        mockkStatic(DocumentsContract::class)
        mockkObject(ContentQueryHelper)
        
        every { context.contentResolver } returns mockk(relaxed = true)
        every { DocumentsContract.getDocumentId(any()) } returns "docId"
        every { DocumentsContract.getTreeDocumentId(any()) } returns "treeId"
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "uri/mock"
        every { DocumentsContract.buildDocumentUriUsingTree(any(), any()) } returns mockUri
        every { DocumentsContract.buildChildDocumentsUriUsingTree(any(), any()) } returns mockUri
        every { ContentQueryHelper.listFiles(any(), any(), any()) } returns Either.Right(emptyList())
        every { ContentQueryHelper.listFiles(any(), any()) } returns Either.Right(emptyList())
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentFile::class)
        unmockkStatic(DocumentsContract::class)
        unmockkObject(ContentQueryHelper)
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshMangaChapters deve deletar e reinserir capitulos quando necessario`() = runTest {
        val mangaId = 1L
        val directory = MangaDirectoryFixtures.createMangaDirectory(id = mangaId, lastModified = 1000L)
        val uri = mockk<Uri>()
        val folderDoc = mockk<DocumentFile>()
        
        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns directory
        every { Uri.parse(any()) } returns uri
        every { DocumentFile.fromSingleUri(context, uri) } returns folderDoc
        
        // Simula que FS é mais novo que DB
        val oldChapter = ChapterArchive(id = 2, chapter = "Old", path = "old/uri", folderPathFk = mangaId, chapterSort = "0")
        coEvery { chapterArchiveDao.getChaptersByMangaDirectoryList(mangaId) } returns listOf(oldChapter)
        every { folderDoc.lastModified() } returns 2000L 
        
        // Simula arquivos
        val ch1 = mockk<DocumentFile>()
        val ch1Uri = mockk<Uri>()
        every { ch1.isFile } returns true
        every { ch1.name } returns "ch1.cbz"
        every { ch1.uri } returns ch1Uri
        every { ch1Uri.toString() } returns "uri/ch1"
        every { ch1.length() } returns 1024L
        every { ch1.lastModified() } returns 3000L
        
        every { folderDoc.listFiles() } returns arrayOf(ch1)
        
        coEvery { chapterArchiveDao.delete(any()) } returns Unit
        coEvery { chapterArchiveDao.insertAll(*anyVararg()) } returns longArrayOf(1)
        coEvery { directoryDao.update(any()) } returns Unit

        // Mock utils
        // NOTE: Como 'sha256' e 'templateToRegex' são extension/top-level, se não mockados rodam real. 
        // sha256 precisa de context. Vamos assumir que funcionam ou mockar se falhar.
        // O código usa 'file.sha256(context)'. Precisa mockar static br.acerola.manga.util.DocumentFileHashKt
        // Para simplificar, vou confiar que DocumentFile mockado aguenta ou o teste falha. 
        // Se falhar, preciso de mockkStatic("br.acerola.manga.util.DocumentFileHashKt").
        // Vou adicionar preventivamente.
        mockkStatic("br.acerola.manga.util.DocumentFileHashKt")
        every { ch1.sha256(context) } returns "hash"
        
        // Mock templateToRegex
        mockkStatic("br.acerola.manga.util.TemplateToRegexKt")
        val regex = Regex("ch(\\d+)\\.cbz")
        every { templateToRegex(any()) } returns regex

        val result = repository.refreshMangaChapters(mangaId)

        assertTrue(result.isRight())
        coVerify { chapterArchiveDao.delete(oldChapter) }
        coVerify { chapterArchiveDao.insertAll(*anyVararg()) }
        coVerify { directoryDao.update(match { it.lastModified == 2000L }) }
        
        unmockkStatic("br.acerola.manga.util.DocumentFileHashKt")
        unmockkStatic("br.acerola.manga.util.TemplateToRegexKt")
    }

    @Test
    fun `refreshMangaChapters deve retornar DiskIOFailure em erro de IO`() = runTest {
        val mangaId = 1L
        coEvery { directoryDao.getMangaDirectoryById(mangaId) } throws IOException("Disk error")

        val result = repository.refreshMangaChapters(mangaId)

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is LibrarySyncError.DiskIOFailure) }
    }
    
    @Test
    fun `observeChapters deve emitir PageDto corretamente`() = runTest {
        val mangaId = 1L
        val chapters = listOf(
            ChapterArchive(id = 1, chapter = "1", path = "path", chapterSort = "1", folderPathFk = mangaId)
        )
        // Simula a emissão da lista de capítulos pelo DAO
        every { chapterArchiveDao.getChaptersByMangaDirectory(mangaId) } returns flowOf(chapters)

        // Coleta o primeiro valor que tenha itens (ignorando o valor inicial vazio do stateIn)
        val result = repository.observeChapters(mangaId).first { it.items.isNotEmpty() }

        assertEquals(1, result.items.size)
        assertEquals("1", result.items[0].name)
    }

    @Test
    fun `getChapterPage deve calcular total e offset corretamente`() = runTest {
        val mangaId = 1L
        coEvery { chapterArchiveDao.countChaptersByMangaDirectory(mangaId) } returns 10
        coEvery { chapterArchiveDao.getChaptersPaged(mangaId, 5, 5) } returns listOf(
            ChapterArchive(id = 2, chapter = "2", path = "path", chapterSort = "2", folderPathFk = mangaId)
        )

        // page 1, size 5 -> offset 5
        val result = repository.getChapterPage(mangaId, total = 0, page = 1, pageSize = 5)

        assertEquals(10, result.total)
        assertEquals(1, result.items.size)
    }
}
