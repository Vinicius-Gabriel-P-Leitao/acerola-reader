package br.acerola.manga.repository.adapter.remote.mangadex.manga

import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MangadexFetchCoverRepositoryTest {

    @MockK lateinit var api: MangadexDownloadApi
    private lateinit var repository: MangadexFetchCoverRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = MangadexFetchCoverRepository(api)
    }

    @Test
    fun `searchCover deve baixar bytes corretamente`() = runTest {
        val url = "http://cover.jpg"
        val bytes = byteArrayOf(1, 2, 3)
        val responseBody = mockk<ResponseBody>()

        every { responseBody.bytes() } returns bytes
        
        coEvery { api.downloadFile(url) } returns responseBody

        val result = repository.searchCover(url)

        assertTrue(result.isRight())
        result.onRight { 
            assertArrayEquals(bytes, it)
        }
    }

    @Test
    fun `searchCover deve retornar ConnectionFailed em erro de rede`() = runTest {
        coEvery { api.downloadFile(any()) } throws IOException("Net error")

        val result = repository.searchCover("url")

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is NetworkError.ConnectionFailed) }
    }
}