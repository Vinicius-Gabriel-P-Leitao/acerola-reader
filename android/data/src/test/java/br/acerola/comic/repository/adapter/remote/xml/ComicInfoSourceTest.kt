package br.acerola.comic.repository.adapter.remote.xml

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.comic.adapter.metadata.comicinfo.source.ComicInfoSource
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.message.NetworkError
import br.acerola.comic.service.metadata.ComicInfoParser
import br.acerola.comic.service.reader.ChapterSourceFactory
import br.acerola.comic.service.reader.contract.PageSource
import io.mockk.MockKAnnotations
import io.mockk.coEvery
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
import java.io.InputStream

class ComicInfoSourceTest {
    @MockK lateinit var context: Context

    @MockK lateinit var parser: ComicInfoParser

    @MockK lateinit var chapterSourceFactory: ChapterSourceFactory

    private lateinit var repository: ComicInfoSource

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = ComicInfoSource(parser, chapterSourceFactory, context)

        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentFile::class)
    }

    @Test
    fun searchInfo_deve_ler_da_raiz_se_ComicInfo_existir() =
        runTest {
            // Arrange
            val folderUri = mockk<Uri>()
            val folderDoc = mockk<DocumentFile>()
            val xmlFile = mockk<DocumentFile>()
            val inputStream = mockk<InputStream>(relaxed = true)
            val expectedInfo = mockk<ComicMetadataDto>()

            every { Uri.parse(any()) } returns folderUri
            every { DocumentFile.fromTreeUri(context, folderUri) } returns folderDoc
            every { folderDoc.exists() } returns true
            every { folderDoc.findFile("ComicInfo.xml") } returns xmlFile
            every { xmlFile.exists() } returns true
            every { xmlFile.uri } returns mockk()

            val resolver = mockk<ContentResolver>()
            every { context.contentResolver } returns resolver
            every { resolver.openInputStream(any()) } returns inputStream
            every { inputStream.close() } returns Unit

            coEvery { parser.parseMangaInfo(inputStream) } returns Either.Right(expectedInfo)

            // Act
            val result = repository.searchInfo("title", onProgress = null, extra = arrayOf("content://folder"))

            // Assert
            assertTrue("Deveria ser Right mas foi $result", result.isRight())
            result.onRight { assertEquals(expectedInfo, it.first()) }
        }

    @Test
    fun searchInfo_deve_procurar_dentro_do_cbz_se_nao_houver_xml_na_raiz() =
        runTest {
            // Arrange
            val folderUri = mockk<Uri>()
            val folderDoc = mockk<DocumentFile>()
            val cbzFile = mockk<DocumentFile>()
            val sourceService = mockk<PageSource>()
            val inputStream = mockk<InputStream>(relaxed = true)
            val expectedInfo = mockk<ComicMetadataDto>()

            every { Uri.parse(any()) } returns folderUri
            every { DocumentFile.fromTreeUri(context, folderUri) } returns folderDoc
            every { folderDoc.exists() } returns true
            every { folderDoc.findFile("ComicInfo.xml") } returns null

            every { folderDoc.listFiles() } returns arrayOf(cbzFile)
            every { cbzFile.isFile } returns true
            every { cbzFile.name } returns "ch1.cbz"
            val cbzUri = mockk<Uri>()
            every { cbzFile.uri } returns cbzUri
            every { cbzUri.toString() } returns "content://ch1.cbz"
            every { cbzFile.lastModified() } returns 0L

            coEvery { chapterSourceFactory.create(any()) } returns Either.Right(sourceService)
            coEvery { sourceService.getFileStream("ComicInfo.xml") } returns Either.Right(inputStream)
            coEvery { parser.parseMangaInfo(inputStream) } returns Either.Right(expectedInfo)
            every { sourceService.close() } returns Unit
            every { inputStream.close() } returns Unit

            // Act
            val result = repository.searchInfo("title", onProgress = null, extra = arrayOf("content://folder"))

            // Assert
            assertTrue("Deveria ser Right mas foi $result", result.isRight())
            result.onRight { assertEquals(expectedInfo, it.first()) }
        }

    @Test
    fun searchInfo_deve_retornar_NotFound_se_diretorio_nao_existir() =
        runTest {
            every { Uri.parse(any()) } returns mockk()
            every { DocumentFile.fromTreeUri(any(), any()) } returns null

            val result = repository.searchInfo("title", onProgress = null, extra = arrayOf("content://fail"))

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is NetworkError.NotFound) }
        }
}
