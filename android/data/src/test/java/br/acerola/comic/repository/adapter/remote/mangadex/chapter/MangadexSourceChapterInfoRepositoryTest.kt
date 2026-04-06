package br.acerola.comic.repository.adapter.remote.mangadex.chapter

import android.content.Context
import br.acerola.comic.adapter.metadata.mangadex.source.MangadexChapterInfoSource
import br.acerola.comic.config.preference.MetadataPreference
import br.acerola.comic.error.message.NetworkError
import br.acerola.comic.pattern.LanguagePattern
import br.acerola.comic.remote.mangadex.api.MangadexChapterMetadataClient
import br.acerola.comic.remote.mangadex.dto.MangaDexResponse
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterAttributes
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterPage
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MangadexSourceChapterInfoRepositoryTest {

    @MockK
    lateinit var api: MangadexChapterMetadataClient
    
    @MockK
    lateinit var context: Context

    private lateinit var repository: MangadexChapterInfoSource

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(MetadataPreference)
        every { MetadataPreference.metadataLanguageFlow(context) } returns flowOf(LanguagePattern.PT_BR.code)
        repository = MangadexChapterInfoSource(api, context)
    }

    @After
    fun tearDown() = unmockkObject(MetadataPreference)

    @Test
    fun `searchInfo deve paginar corretamente ate buscar todos os capitulos`() = runTest {
        // Arrange
        val mangaId = "comic-1"
        val listA = List(100) { createChapterDto("A$it") }
        val listB = List(50) { createChapterDto("B$it") }

        val respA = createResponse(listA, total = 150, offset = 0)
        val respB = createResponse(listB, total = 150, offset = 100)
        
        val languages = listOf(LanguagePattern.PT_BR.code)

        // Mock inicial
        coEvery { api.getMangaFeed(mangaId, languages = languages, limit = 1, offset = 0) } returns createResponse(
            listOf(listA[0]),
            total = 150
        )

        // Mock paginação
        coEvery { api.getMangaFeed(mangaId, languages = languages, limit = 100, offset = 0) } returns respA
        coEvery { api.getMangaFeed(mangaId, languages = languages, limit = 100, offset = 100) } returns respB

        // Mock imagem do comic
        coEvery { api.getChapterImages(any()) } returns ChapterSourceMangadexDto(
            "url",
            ChapterPage("hash", emptyList())
        )

        // Act
        val result = repository.searchInfo(mangaId, limit = 100, offset = 0, onProgress = {})

        // Assert
        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(150, list.size)
        }

        coVerify { api.getMangaFeed(mangaId, languages = languages, limit = 100, offset = 0) }
        coVerify { api.getMangaFeed(mangaId, languages = languages, limit = 100, offset = 100) }
    }

    @Test
    fun `searchInfo deve retornar ConnectionFailed quando ocorrer erro de IO`() = runTest {
        val mangaId = "comic-1"
        val languages = listOf(LanguagePattern.PT_BR.code)
        // Simula erro de conexão (IOException)
        coEvery { api.getMangaFeed(any(), languages = languages, limit = 1, offset = 0) } throws IOException("Network Failure")

        val result = repository.searchInfo(mangaId, limit = 100)

        assertTrue(result.isLeft())
        // Valida se o erro é do tipo NetworkError (qualquer subclasse)
        result.onLeft { error ->
            assertTrue("Erro deve ser do tipo NetworkError, recebido: $error", error is NetworkError)
        }
    }

    private fun createChapterDto(id: String) = ChapterMangadexDto(
        id = id, type = "chapter",
        attributes = ChapterAttributes(null, "1", "Title", pages = 10, version = 1)
    )

    private fun <T> createResponse(data: List<T>, total: Int, offset: Int = 0, limit: Int = 100) = MangaDexResponse(
        result = "ok", response = "collection", data = data, limit = limit, offset = offset, total = total
    )
}
