package br.acerola.comic.repository.adapter.local.chapter

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.adapter.library.engine.ChapterArchiveEngine
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.relation.ChapterVolumeJoin
import br.acerola.comic.service.archive.ArchiveValidator
import br.acerola.comic.service.archive.ChapterSyncService
import br.acerola.comic.service.archive.VolumeSyncService
import br.acerola.comic.service.compact.PdfToCbzConverter
import br.acerola.comic.service.template.ChapterNameProcessor
import br.acerola.comic.util.file.ContentQueryHelper
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChapterArchiveEngineTest {
    @MockK
    lateinit var directoryDao: ComicDirectoryDao

    @MockK
    lateinit var chapterArchiveDao: ChapterArchiveDao

    @MockK
    lateinit var templateService: ChapterNameProcessor

    @MockK
    lateinit var pdfToCbzConverterService: PdfToCbzConverter

    @MockK
    lateinit var archiveValidator: ArchiveValidator

    @MockK
    lateinit var volumeSyncService: VolumeSyncService

    @MockK
    lateinit var chapterSyncService: ChapterSyncService

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
                templateService,
                context,
                pdfToCbzConverterService,
                archiveValidator,
                volumeSyncService,
                chapterSyncService,
            )

        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
        mockkStatic(DocumentsContract::class)
        mockkObject(ContentQueryHelper)

        every { context.contentResolver } returns mockk(relaxed = true)
        coEvery { templateService.getTemplates() } returns emptyList()
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
    fun `refreshComicChapters deve orquestrar o sync corretamente`() =
        runTest {
            val comicId = 1L
            val directory = MangaDirectoryFixtures.createMangaDirectory(id = comicId, lastModified = 1000L)

            val uriMock = mockk<Uri>()
            every { Uri.parse(any()) } returns uriMock

            val folderDoc = mockk<DocumentFile>()
            every { DocumentFile.fromSingleUri(context, uriMock) } returns folderDoc
            every { folderDoc.listFiles() } returns emptyArray()
            every { folderDoc.lastModified() } returns 2000L

            coEvery { directoryDao.getDirectoryById(comicId) } returns directory
            coEvery { volumeSyncService.sync(any(), any(), any(), any(), any()) } returns emptyMap()
            coEvery { chapterSyncService.sync(any(), any(), any(), any(), any(), any()) } returns Unit
            coEvery { directoryDao.update(any()) } returns Unit

            val result = repository.refreshComicChapters(comicId)

            assertTrue("Deveria ser Right mas foi $result", result.isRight())
            coVerify { volumeSyncService.sync(eq(comicId), any(), any(), any(), any()) }
            coVerify { chapterSyncService.sync(eq(comicId), any(), any(), any(), any(), any()) }
            coVerify { directoryDao.update(match { it.lastModified == 2000L }) }
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
            every { chapterArchiveDao.getChaptersByDirectoryId(comicId) } returns flowOf(chapters)

            val result = repository.observeChapters(comicId).first { it.items.isNotEmpty() }

            assertEquals(1, result.items.size)
            assertEquals("1", result.items[0].name)
        }

    @Test
    fun `getChapterPage deve retornar pagina de capitulos corretamente`() =
        runTest {
            val comicId = 1L
            coEvery { chapterArchiveDao.countByDirectoryId(comicId) } returns 10
            coEvery { chapterArchiveDao.getChaptersByDirectoryPaged(comicId, 5, 0) } returns
                listOf(
                    ChapterVolumeJoin(
                        chapter = ChapterArchive(id = 1, chapter = "1", path = "path", chapterSort = "1", folderPathFk = comicId),
                        volume = null,
                    ),
                )

            val result = repository.getChapterPage(comicId, total = 0, page = 0, pageSize = 5)

            assertEquals(10, result.total)
            assertEquals(1, result.items.size)
            coVerify { chapterArchiveDao.getChaptersByDirectoryPaged(comicId, 5, 0) }
        }
}
