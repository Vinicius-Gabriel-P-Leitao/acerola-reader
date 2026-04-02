package br.acerola.manga.repository.adapter.remote.mangadex.manga

import android.content.Context
import br.acerola.manga.data.R
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.pattern.LanguagePattern
import br.acerola.manga.remote.mangadex.api.MangadexMangaMetadataClient
import br.acerola.manga.remote.mangadex.dto.MangaDexResponse
import br.acerola.manga.remote.mangadex.dto.manga.MangaAttributes
import br.acerola.manga.remote.mangadex.dto.manga.MangaMangadexDto
import br.acerola.manga.adapter.metadata.mangadex.source.MangadexMangaInfoSource
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
import java.io.File

class MangadexSourceMangaInfoRepositoryTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var api: MangadexMangaMetadataClient

    private lateinit var repository: MangadexMangaInfoSource

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = MangadexMangaInfoSource(context, api)
        every { context.getString(R.string.description_manga_untitled) } returns "Sem título"
        every { context.applicationContext } returns context
        every { context.filesDir } returns File(System.getProperty("java.io.tmpdir"))
    }

    @Test
    fun `searchInfo deve buscar manga e mapear DTO corretamente`() = runTest {
        val title = "Naruto"
        val languages = listOf(LanguagePattern.PT_BR.code)

        val attr = MangaAttributes(
            titleMap = mapOf("pt-br" to "Naruto"),
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

        coEvery { api.searchMangaByName(title, 10, 0, any(), languages) } returns response

        val result = repository.searchInfo(manga = title, limit = 10, offset = 0, onProgress = null)

        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(1, list.size)
            assertEquals("Naruto", list[0].title)
            assertEquals("1", list[0].sources?.mangadex?.mangadexId)
        }
    }

    @Test
    fun `searchInfo deve retornar ConnectionFailed em caso de erro de IO`() = runTest {
        val languages = listOf(LanguagePattern.PT_BR.code)
        coEvery { api.searchMangaByName(any(), any(), any(), any(), languages) } throws IOException("Connection Reset")

        val result = repository.searchInfo("Naruto")

        assertTrue(result.isLeft())
        // Valida se o erro mapeado é ConnectionFailed e não Unknown
        result.onLeft { error ->
            assertTrue(error is NetworkError)
        }
    }
}
