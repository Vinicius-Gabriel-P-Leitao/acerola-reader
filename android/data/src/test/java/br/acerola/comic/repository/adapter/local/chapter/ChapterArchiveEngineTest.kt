package br.acerola.comic.repository.adapter.local.chapter

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.comic.adapter.library.engine.ChapterArchiveEngine
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.archive.VolumeArchiveDao
import br.acerola.comic.local.dao.history.ReadingHistoryDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.relation.ChapterVolumeJoin
import br.acerola.comic.service.compact.PdfToCbzConverter
import br.acerola.comic.service.template.ChapterNameProcessor
import br.acerola.comic.util.file.ContentQueryHelper
import br.acerola.comic.util.file.FastFileMetadata
import br.acerola.comic.util.file.sha256
import br.acerola.comic.util.sort.SortNormalizer
import br.acerola.comic.util.sort.SortResult
import br.acerola.comic.util.sort.SortType
import br.acerola.comic.util.template.templateToRegex
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChapterArchiveEngineTest {
    @MockK
    lateinit var directoryDao: ComicDirectoryDao

    @MockK
    lateinit var chapterArchiveDao: ChapterArchiveDao

    @MockK
    lateinit var volumeArchiveDao: VolumeArchiveDao

    @MockK
    lateinit var readingHistoryDao: ReadingHistoryDao

    @MockK
    lateinit var templateService: ChapterNameProcessor

    @MockK
    lateinit var pdfToCbzConverterService: PdfToCbzConverter

    @MockK
    lateinit var context: Context

    private lateinit var repository: ChapterArchiveEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository =
            ChapterArchiveEngine(
                directoryDao,
                chapterArchiveDao,
                volumeArchiveDao,
                readingHistoryDao,
                templateService,
                context,
                pdfToCbzConverterService,
            )

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
        coEvery { templateService.getTemplates() } returns emptyList()
        coEvery { readingHistoryDao.updateHistoryChapterIdBySort(any(), any(), any()) } returns Unit
        coEvery { readingHistoryDao.updateChapterReadIdBySort(any(), any(), any()) } returns Unit
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
    fun `refreshMangaChapters deve deletar e reinserir capitulos quando necessario`() =
        runTest {
            val comicId = 1L
            val directory = MangaDirectoryFixtures.createMangaDirectory(id = comicId, lastModified = 1000L)

            val uriMock = mockk<Uri>()
            every { Uri.parse(any()) } returns uriMock

            val folderDoc = mockk<DocumentFile>()
            every { DocumentFile.fromSingleUri(context, uriMock) } returns folderDoc
            every { DocumentFile.fromTreeUri(context, uriMock) } returns folderDoc
            every { DocumentsContract.getDocumentId(uriMock) } returns "folder_doc_id"

            coEvery { directoryDao.getDirectoryById(comicId) } returns directory
            coEvery { volumeArchiveDao.getVolumesListByDirectoryId(comicId) } returns emptyList()

            // Simula que FS é mais novo que DB
            val oldChapter = ChapterArchive(id = 2, chapter = "Old", path = "old/uri", folderPathFk = comicId, chapterSort = "0")
            coEvery { chapterArchiveDao.getChaptersListByDirectoryId(comicId) } returns listOf(oldChapter)
            every { folderDoc.lastModified() } returns 2000L

            // Simula arquivos
            val ch1Metadata =
                FastFileMetadata(
                    id = "ch1",
                    name = "ch1.cbz",
                    size = 1024L,
                    mimeType = "application/x-cbz",
                    lastModified = 3000L,
                )

            every { ContentQueryHelper.listFiles(any(), any()) } returns Either.Right(listOf(ch1Metadata))

            // Mock individual DocumentFile for hash calculation
            val ch1 = mockk<DocumentFile>()
            every { ch1.isFile } returns true
            every { ch1.isDirectory } returns false
            every { ch1.name } returns "ch1.cbz"
            every { ch1.type } returns "application/x-cbz"
            val ch1Uri = mockk<Uri>()
            every { ch1Uri.toString() } returns "uri/ch1"
            every { ch1.uri } returns ch1Uri
            every { ch1.length() } returns 1024L
            every { ch1.lastModified() } returns 3000L

            every { folderDoc.listFiles() } returns arrayOf(ch1)
            every { DocumentFile.fromSingleUri(context, ch1Uri) } returns ch1
            every { DocumentsContract.getDocumentId(ch1Uri) } returns "ch1"

            coEvery { chapterArchiveDao.delete(any()) } returns Unit
            coEvery { chapterArchiveDao.insert(any()) } returns 1L
            coEvery { directoryDao.update(any()) } returns Unit

            // Mock utils
            mockkStatic("br.acerola.comic.util.file.FileHashKt")
            every { ch1.sha256(context) } returns "hash"

            // Mock templateToRegex
            mockkStatic("br.acerola.comic.util.template.TemplateConverterKt")
            val regex = Regex("ch(\\d+)\\.cbz")
            every { templateToRegex(any()) } returns regex

            // Mock SortNormalizer
            mockkObject(SortNormalizer)
            every { SortNormalizer.normalize("ch1.cbz", SortType.CHAPTER, any()) } returns SortResult(SortType.CHAPTER, 1, 0, false, "1")

            val result = repository.refreshComicChapters(comicId)

            assertTrue("Expected Right but got $result", result.isRight())
            coVerify { chapterArchiveDao.delete(oldChapter) }
            coVerify { chapterArchiveDao.insert(any()) }
            coVerify { directoryDao.update(match { it.lastModified == 2000L }) }

            unmockkObject(SortNormalizer)
            unmockkStatic("br.acerola.comic.util.file.FileHashKt")
            unmockkStatic("br.acerola.comic.util.template.TemplateConverterKt")
        }

    @Test
    fun `refreshMangaChapters deve retornar DiskIOFailure em erro de IO`() =
        runTest {
            val comicId = 1L
            coEvery { directoryDao.getDirectoryById(comicId) } throws IOException("Disk error")

            val result = repository.refreshComicChapters(comicId)

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is LibrarySyncError.DiskIOFailure) }
        }

    @Test
    fun `observeChapters deve emitir PageDto corretamente`() =
        runTest {
            val comicId = 1L
            val chapters =
                listOf(
                    ChapterVolumeJoin(
                        chapter = ChapterArchive(id = 1, chapter = "1", path = "path", chapterSort = "1", folderPathFk = comicId),
                        volume = null,
                    ),
                )
            // Simula a emissão da lista de capítulos pelo DAO
            every { chapterArchiveDao.getChaptersByDirectoryId(comicId) } returns flowOf(chapters)

            // Coleta o primeiro valor que tenha itens (ignorando o valor inicial vazio do stateIn)
            val result = repository.observeChapters(comicId).first { it.items.isNotEmpty() }

            assertEquals(1, result.items.size)
            assertEquals("1", result.items[0].name)
        }

    @Test
    fun `getChapterPage deve calcular total e offset corretamente para ordem ascendente`() =
        runTest {
            val comicId = 1L
            coEvery { chapterArchiveDao.countByDirectoryId(comicId) } returns 10
            coEvery { chapterArchiveDao.getChaptersByDirectoryPaged(comicId, 5, 5) } returns
                listOf(
                    ChapterVolumeJoin(
                        chapter = ChapterArchive(id = 2, chapter = "2", path = "path", chapterSort = "2", folderPathFk = comicId),
                        volume = null,
                    ),
                )

            // page 1, size 5 -> offset 5, isAscending = true (default)
            val result = repository.getChapterPage(comicId, total = 0, page = 1, pageSize = 5)

            assertEquals(10, result.total)
            assertEquals(1, result.items.size)
            coVerify { chapterArchiveDao.getChaptersByDirectoryPaged(comicId, 5, 5) }
        }

    @Test
    fun `getChapterPage deve chamar query descendente quando isAscending for falso`() =
        runTest {
            val comicId = 1L
            coEvery { chapterArchiveDao.countByDirectoryId(comicId) } returns 10
            coEvery { chapterArchiveDao.getChaptersByDirectoryPagedDesc(comicId, 5, 0) } returns
                listOf(
                    ChapterVolumeJoin(
                        chapter = ChapterArchive(id = 10, chapter = "10", path = "path", chapterSort = "10", folderPathFk = comicId),
                        volume = null,
                    ),
                )

            // page 0, size 5, isAscending = false
            val result = repository.getChapterPage(comicId, total = 0, page = 0, pageSize = 5, isAscending = false)

            assertEquals(1, result.items.size)
            assertEquals("10", result.items[0].name)
            coVerify { chapterArchiveDao.getChaptersByDirectoryPagedDesc(comicId, 5, 0) }
        }
}
