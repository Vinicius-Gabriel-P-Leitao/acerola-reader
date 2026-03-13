package br.acerola.manga.repository.adapter.remote.mangadex.manga

import android.content.Context
import br.acerola.manga.data.R
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.remote.mangadex.api.MangadexMangaInfoApi
import br.acerola.manga.remote.mangadex.dto.MangaDexResponse
import br.acerola.manga.remote.mangadex.dto.manga.MangaAttributes
import br.acerola.manga.remote.mangadex.dto.manga.MangaMangadexDto
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MangadexMangaInfoRepositoryTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var api: MangadexMangaInfoApi

    private lateinit var repository: MangadexMangaInfoRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = MangadexMangaInfoRepository(context, api)
        every { context.getString(R.string.description_manga_untitled) } returns "Sem título"
    }

    @Test
    fun `searchInfo deve buscar manga e mapear DTO corretamente`() = runTest {
        val title = "Naruto"

        val attr = MangaAttributes(
            titleMap = mapOf(pair = "en" to "Naruto"),
            altTitlesList = emptyList(),
            descriptionMap = emptyMap(),
            isLocked = false,
            links = null,
            status = "ongoing"
        )

        val mangaDto = MangaMangadexDto(
            id = "1",
            type = "manga",
            attributes = attr,
            relationships = emptyList()
        )

        val response = MangaDexResponse(
            result = "ok",
            response = "collection",
            data = listOf(mangaDto),
            limit = 10,
            offset = 0,
            total = 1
        )

        coEvery { api.searchMangaByName(title, limit = 10, offset = 0, includes = any()) } returns response

        val result = repository.searchInfo(manga = title, limit = 10, offset = 0, onProgress = null)

        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(1, list.size)
            assertEquals("Naruto", list[0].title)
            assertEquals("1", list[0].mirrorId)
        }
    }

    @Test
    fun `searchInfo deve retornar ConnectionFailed em caso de erro de IO`() = runTest {
        // Simula erro de conexão (IOException)
        coEvery { api.searchMangaByName(any(), any(), any(), any()) } throws IOException("Connection Reset")

        val result = repository.searchInfo("Naruto")

        assertTrue(result.isLeft())
        // WARN: Valida se o erro mapeado é ConnectionFailed e não Unknown
        result.onLeft { error ->
            assertTrue("Erro deve ser ConnectionFailed", error is NetworkError.ConnectionFailed)
        }
    }
}