package br.acerola.comic.repository.adapter.remote.xml

import android.content.Context
import arrow.core.Either
import br.acerola.comic.adapter.metadata.comicinfo.source.ChapterComicInfoSource
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.error.message.NetworkError
import br.acerola.comic.service.metadata.ComicInfoParser
import br.acerola.comic.service.reader.ChapterSourceFactory
import br.acerola.comic.service.reader.contract.PageSource
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStream

class ChapterComicInfoSourceRepositoryTest {
    @MockK lateinit var context: Context

    @MockK lateinit var parser: ComicInfoParser

    @MockK lateinit var chapterSourceFactory: ChapterSourceFactory

    private lateinit var repository: ChapterComicInfoSource

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = ChapterComicInfoSource(context, parser, chapterSourceFactory)
    }

    @Test
    fun searchInfo_deve_extrair_ComicInfo_de_dentro_do_arquivo_do_capitulo() =
        runTest {
            val chapterUri = "content://comic/ch1.cbz"
            val sourceService = mockk<PageSource>()
            val inputStream = mockk<InputStream>(relaxed = true)
            val expectedInfo = ChapterMetadataDto(id = "1", chapter = "1", mangadexVersion = 0)

            coEvery { chapterSourceFactory.create(any()) } returns Either.Right(sourceService)
            coEvery { sourceService.getFileStream("ComicInfo.xml") } returns Either.Right(inputStream)
            coEvery { parser.parseChapterInfo(inputStream) } returns Either.Right(expectedInfo)
            every { sourceService.close() } returns Unit

            val result = repository.searchInfo(manga = chapterUri, onProgress = null)

            assertTrue("Deveria retornar sucesso, mas foi $result", result.isRight())
            result.onRight { list ->
                assertEquals(1, list.size)
                assertEquals("1", list[0].chapter)
            }
        }

    @Test
    fun searchInfo_deve_retornar_NotFound_se_arquivo_nao_contiver_ComicInfo() =
        runTest {
            val sourceService = mockk<PageSource>()
            coEvery { chapterSourceFactory.create(any()) } returns Either.Right(sourceService)
            coEvery { sourceService.getFileStream("ComicInfo.xml") } returns Either.Left(mockk())
            every { sourceService.close() } returns Unit

            val result = repository.searchInfo(manga = "path.cbz", onProgress = null)

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is NetworkError.NotFound) }
        }
}
