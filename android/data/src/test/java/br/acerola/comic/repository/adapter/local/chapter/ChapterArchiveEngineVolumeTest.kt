package br.acerola.comic.repository.adapter.local.chapter

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.comic.adapter.library.engine.ChapterArchiveEngine
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.service.archive.ArchiveValidator
import br.acerola.comic.service.archive.ChapterSyncService
import br.acerola.comic.service.archive.VolumeSyncService
import br.acerola.comic.service.compact.PdfToCbzConverter
import br.acerola.comic.service.template.ChapterNameProcessor
import br.acerola.comic.util.file.ContentQueryHelper
import br.acerola.comic.util.file.FastFileMetadata
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChapterArchiveEngineVolumeTest {
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

        every { DocumentsContract.getDocumentId(any()) } returns "primary:root"
        every { DocumentsContract.buildDocumentUriUsingTree(any<Uri>(), any<String>()) } returns mockk(relaxed = true)
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
    fun `refreshComicChapters deve converter PDF para CBZ dentro de subpasta de volume e atualizar`() =
        runTest {
            val comicId = 1L
            val validPath = "content://com.android.externalstorage.documents/tree/primary%3Aroot"
            val comicDir = MangaDirectoryFixtures.createMangaDirectory(id = comicId, path = validPath)

            val folderUri = mockk<Uri>()
            every { Uri.parse(any()) } returns folderUri
            every { folderUri.toString() } returns validPath

            val folderDoc = mockk<DocumentFile>(relaxed = true)
            every { DocumentFile.fromSingleUri(context, folderUri) } returns folderDoc
            every { DocumentFile.fromTreeUri(context, folderUri) } returns folderDoc
            every { folderDoc.exists() } returns true

            val pdfFile = FastFileMetadata("pdf_id", 1024L, "Ch 01.pdf", "application/pdf", 1000L)
            val volumeFolder = FastFileMetadata("vol_id", 0L, "Vol 01", DocumentsContract.Document.MIME_TYPE_DIR, 1000L)

            val volumeFolderDoc = mockk<DocumentFile>(relaxed = true)
            every { volumeFolderDoc.name } returns "Vol 01"
            every { volumeFolderDoc.type } returns DocumentsContract.Document.MIME_TYPE_DIR
            every { volumeFolderDoc.uri } returns mockk(relaxed = true)

            every { folderDoc.listFiles() } returns arrayOf(volumeFolderDoc)

            every { ContentQueryHelper.listFiles(any(), any(), eq("primary:root")) } returns Either.Right(listOf(volumeFolder))
            coEvery { directoryDao.getDirectoryById(any()) } returns comicDir
            coEvery { volumeSyncService.sync(any(), any(), any(), any(), any()) } returns mapOf("vol_uri" to 101L)

            // Mocking the check for PDF conversion
            val volDoc = mockk<DocumentFile>(relaxed = true)
            every { volDoc.exists() } returns true
            every { folderDoc.findFile(any()) } returns volDoc
            every { ContentQueryHelper.listFiles(any(), any(), eq("vol_id")) } returns Either.Right(listOf(pdfFile))
            every { archiveValidator.isPdfConversionEligible(any(), any(), any()) } returns true

            val pdfDoc = mockk<DocumentFile>(relaxed = true)
            every { DocumentFile.fromSingleUri(any(), any()) } returns pdfDoc
            coEvery { pdfToCbzConverterService.convertPdfToCbz(any(), any(), any()) } returns Either.Right(Unit)

            coEvery { chapterSyncService.sync(any(), any(), any(), any(), any(), any()) } returns Unit

            val result = repository.refreshComicChapters(comicId, folderUri)

            assertTrue("Expected Right but was $result", result.isRight())
            coVerify { pdfToCbzConverterService.convertPdfToCbz(any(), any(), any()) }
            coVerify { chapterSyncService.sync(any(), any(), any(), any(), any(), any()) }
        }
}
