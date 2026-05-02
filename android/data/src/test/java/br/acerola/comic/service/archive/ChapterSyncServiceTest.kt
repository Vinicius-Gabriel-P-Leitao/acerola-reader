package br.acerola.comic.service.archive

import android.net.Uri
import android.provider.DocumentsContract
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.history.ReadingHistoryDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.util.file.FastFileMetadata
import br.acerola.comic.util.sort.SortNormalizer
import br.acerola.comic.util.sort.SortResult
import br.acerola.comic.util.sort.SortType
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChapterSyncServiceTest {
    @MockK
    lateinit var chapterArchiveDao: ChapterArchiveDao

    @MockK
    lateinit var readingHistoryDao: ReadingHistoryDao

    @MockK
    lateinit var archiveValidator: ArchiveValidator

    @MockK
    lateinit var chapterIndexer: ChapterIndexer

    private lateinit var service: ChapterSyncService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        service =
            ChapterSyncService(
                chapterArchiveDao,
                readingHistoryDao,
                archiveValidator,
                chapterIndexer,
            )
        mockkObject(SortNormalizer)
        mockkStatic(Uri::class)
        mockkStatic(DocumentsContract::class)
    }

    @After
    fun tearDown() {
        unmockkObject(SortNormalizer)
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentsContract::class)
    }

    @Test
    fun `sync deve deletar capitulos antigos e inserir novos corretamente`() =
        runTest {
            val comicId = 1L
            val file = FastFileMetadata("id", 100L, "cap1.cbz", "mime", 1000L)
            val allChapterFiles = listOf(file to null)
            val chapterTemplates = emptyList<String>()
            val folderUri = mockk<Uri>()
            val fileUriMock = mockk<Uri>()

            every { folderUri.toString() } returns "folder/uri"
            every { fileUriMock.toString() } returns "new/path"
            every { DocumentsContract.buildDocumentUriUsingTree(any(), any()) } returns fileUriMock

            every { SortNormalizer.normalize(any(), any(), any()) } returns SortResult(SortType.CHAPTER, 1, 0, false, "1")
            every { archiveValidator.isDuplicateSort(any(), any()) } returns false

            val oldChapter = ChapterArchive(id = 99, chapter = "Old", path = "old/path", folderPathFk = comicId, chapterSort = "0")
            coEvery { chapterArchiveDao.getChaptersListByDirectoryId(comicId) } returns listOf(oldChapter)

            val newChapter = ChapterArchive(id = 0, chapter = "cap1.cbz", path = "new/path", folderPathFk = comicId, chapterSort = "1")
            every { chapterIndexer.buildEntity(any(), any(), any(), any(), any(), any(), any()) } returns newChapter

            coEvery { chapterArchiveDao.delete(any()) } returns Unit
            coEvery { chapterArchiveDao.insert(any()) } returns 100L
            coEvery { readingHistoryDao.updateHistoryChapterIdBySort(any(), any(), any()) } returns Unit
            coEvery { readingHistoryDao.updateChapterReadIdBySort(any(), any(), any()) } returns Unit

            service.sync(
                comicId = comicId,
                allChapterFiles = allChapterFiles,
                chapterTemplates = chapterTemplates,
                baseUri = null,
                folderUri = folderUri,
                onProgress = {},
            )

            coVerify { chapterArchiveDao.delete(oldChapter) }
            coVerify { chapterArchiveDao.insert(newChapter) }
            coVerify { readingHistoryDao.updateHistoryChapterIdBySort(comicId, "1", 100L) }
        }

    @Test
    fun `sync deve ignorar capitulos duplicados baseados no sort`() =
        runTest {
            val comicId = 1L
            val file1 = FastFileMetadata("id1", 100L, "cap1.cbz", "mime", 1000L)
            val file2 = FastFileMetadata("id2", 100L, "cap1_copy.cbz", "mime", 1000L)
            val allChapterFiles = listOf(file1 to null, file2 to null)
            val folderUri = mockk<Uri>()
            val fileUriMock = mockk<Uri>()

            every { folderUri.toString() } returns "folder/uri"
            every { fileUriMock.toString() } returns "some/path"
            every { DocumentsContract.buildDocumentUriUsingTree(any(), any()) } returns fileUriMock

            every { SortNormalizer.normalize(any(), any(), any()) } returns SortResult(SortType.CHAPTER, 1, 0, false, "1")
            every { archiveValidator.isDuplicateSort(any(), "1") } returnsMany listOf(false, true)

            coEvery { chapterArchiveDao.getChaptersListByDirectoryId(comicId) } returns emptyList()
            every { chapterIndexer.buildEntity(any(), any(), any(), any(), any(), any(), any()) } returns mockk(relaxed = true)
            coEvery { chapterArchiveDao.insert(any()) } returns 1L
            coEvery { readingHistoryDao.updateHistoryChapterIdBySort(any(), any(), any()) } returns Unit
            coEvery { readingHistoryDao.updateChapterReadIdBySort(any(), any(), any()) } returns Unit

            service.sync(
                comicId = comicId,
                allChapterFiles = allChapterFiles,
                chapterTemplates = emptyList(),
                baseUri = null,
                folderUri = folderUri,
                onProgress = {},
            )

            coVerify(exactly = 1) { chapterArchiveDao.insert(any()) }
        }
}
