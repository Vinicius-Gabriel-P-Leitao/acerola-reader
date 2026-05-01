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
import br.acerola.comic.service.compact.PdfToCbzConverter
import br.acerola.comic.service.template.ChapterNameProcessor
import br.acerola.comic.util.file.ContentQueryHelper
import br.acerola.comic.util.file.FastFileMetadata
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
        mockkStatic("br.acerola.comic.util.template.TemplateConverterKt")
        mockkObject(SortNormalizer)

        every { context.contentResolver } returns mockk(relaxed = true)
        coEvery { templateService.getTemplates() } returns
            listOf(
                br.acerola.comic.local.entity.archive.ArchiveTemplate(
                    id = 1L,
                    label = "Default",
                    pattern = "Ch. {chapter}{decimal}.*.{extension}",
                    type = SortType.CHAPTER,
                ),
            )
        coEvery { readingHistoryDao.updateHistoryChapterIdBySort(any(), any(), any()) } returns Unit
        coEvery { readingHistoryDao.updateChapterReadIdBySort(any(), any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentFile::class)
        unmockkStatic(DocumentsContract::class)
        unmockkObject(ContentQueryHelper)
        unmockkStatic(::templateToRegex)
        unmockkObject(SortNormalizer)
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshComicChapters deve converter PDF para CBZ dentro de subpasta de volume usando findFile`() =
        runTest {
            val comicId = 1L
            // URI válida para SAF
            val validPath = "content://com.android.externalstorage.documents/tree/primary%3Aroot/document/primary%3Aroot"
            val comicDir = MangaDirectoryFixtures.createMangaDirectory(id = comicId, path = validPath).copy(archiveTemplateFk = 1L)

            val folderUri = Uri.parse(validPath)
            val baseUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Aroot")

            val folderDoc = mockk<DocumentFile>()
            every { folderDoc.name } returns "Root"

            coEvery { directoryDao.getDirectoryById(comicId = comicId) } returns comicDir

            // Forçamos o mock de IDs para que o código interno não tente validar a URI "na mão"
            every { DocumentsContract.getDocumentId(folderUri) } returns "primary:root"
            every { DocumentsContract.getTreeDocumentId(folderUri) } returns "primary:root"
            every { DocumentsContract.isDocumentUri(any(), any()) } returns true
            every { DocumentFile.fromTreeUri(context, folderUri) } returns folderDoc

            // 1. Detecção de Volumes
            val volumeMetadata =
                FastFileMetadata(
                    id = "primary:root/Vol. 01",
                    name = "Vol. 01",
                    size = 0,
                    mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
                    lastModified = 1000L,
                )
            every { ContentQueryHelper.listFiles(context, baseUri, "primary:root") } returns Either.Right(listOf(volumeMetadata))
            coEvery { volumeArchiveDao.getVolumesListByDirectoryId(comicId) } returns emptyList()
            every { SortNormalizer.normalize("Vol. 01", SortType.VOLUME, any()) } returns SortResult(SortType.VOLUME, 1, 0, false, "1")

            val volUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Aroot/document/primary%3Aroot%2FVol.01")
            every { DocumentsContract.buildDocumentUriUsingTree(baseUri, "primary:root/Vol. 01") } returns volUri
            every { DocumentsContract.getDocumentId(volUri) } returns "primary:root/Vol. 01"
            coEvery { volumeArchiveDao.insert(any()) } returns 101L

            // 2. Detecção de PDF dentro do volume
            val pdfMetadata =
                FastFileMetadata(
                    id = "primary:root/Vol. 01/Ch. 01.pdf",
                    name = "Ch. 01.pdf",
                    size = 1024L,
                    mimeType = "application/pdf",
                    lastModified = 2000L,
                )

            val cbzMetadata =
                FastFileMetadata(
                    id = "primary:root/Vol. 01/Ch. 01.cbz",
                    name = "Ch. 01.cbz",
                    size = 1024L,
                    mimeType = "application/x-cbz",
                    lastModified = 3000L,
                )

            // O engine lê a subpasta algumas vezes antes da conversão e novamente depois do refresh global.
            every { ContentQueryHelper.listFiles(context, baseUri, "primary:root/Vol. 01") } returnsMany
                listOf(
                    Either.Right(listOf(pdfMetadata)), // Step 1: Detect volume media
                    Either.Right(listOf(pdfMetadata)), // Step 2: Determine template
                    Either.Right(listOf(pdfMetadata)), // Step 3: Check conversion
                    Either.Right(listOf(cbzMetadata)), // Step 4: Collect after refresh
                )

            val volDoc = mockk<DocumentFile>()
            every { volDoc.name } returns "Vol. 01"
            every { folderDoc.findFile("Vol. 01") } returns volDoc

            // 3. Expectativas de Conversão
            // Não precisamos mais mockar templateToRegex pois fornecemos um template que o real templateToRegex entende
            val pdfDoc = mockk<DocumentFile>()
            every { pdfDoc.name } returns "Ch. 01.pdf"
            val pdfDocUri =
                Uri.parse(
                    "content://com.android.externalstorage.documents/tree/primary%3Aroot/document/primary%3Aroot%2FVol.01%2FCh.01.pdf",
                )
            every { DocumentsContract.buildDocumentUriUsingTree(baseUri, "primary:root/Vol. 01/Ch. 01.pdf") } returns pdfDocUri
            every { DocumentsContract.getDocumentId(pdfDocUri) } returns "primary:root/Vol. 01/Ch. 01.pdf"
            every { DocumentFile.fromSingleUri(context, pdfDocUri) } returns pdfDoc

            val cbzDocUri =
                Uri.parse(
                    "content://com.android.externalstorage.documents/tree/primary%3Aroot/document/primary%3Aroot%2FVol.01%2FCh.01.cbz",
                )
            every { DocumentsContract.buildDocumentUriUsingTree(baseUri, "primary:root/Vol. 01/Ch. 01.cbz") } returns cbzDocUri

            coEvery { pdfToCbzConverterService.convertPdfToCbz(volDoc, pdfDoc, "Ch. 01.cbz") } returns Either.Right(Unit)

            // 4. Coleta final após refresh (needsGlobalRefresh será true pois converteu)
            // Mock do refresh global
            every { ContentQueryHelper.listFiles(context, baseUri, "primary:root") } returns Either.Right(listOf(volumeMetadata))

            coEvery { chapterArchiveDao.getChaptersListByDirectoryId(folderId = comicId) } returns emptyList()
            coEvery { chapterArchiveDao.insert(any()) } returns 1L
            every { SortNormalizer.normalize("Ch. 01.cbz", SortType.CHAPTER, any()) } returns SortResult(SortType.CHAPTER, 1, 0, false, "1")
            // Execução
            val result = repository.refreshComicChapters(comicId, baseUri)

            // Verificação
            result.onLeft { error ->
                val message =
                    when (error) {
                        is LibrarySyncError.UnexpectedError -> "Unexpected: ${error.cause?.message}"
                        else -> error.toString()
                    }
                throw AssertionError("Deveria ser Right, mas retornou Left: $message")
            }
            assertTrue("Deveria ser Right", result.isRight())
            coVerify { pdfToCbzConverterService.convertPdfToCbz(volDoc, pdfDoc, "Ch. 01.cbz") }
            coVerify { folderDoc.findFile("Vol. 01") }
        }
}
