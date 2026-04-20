package br.acerola.comic.remote.mangadex.interceptor

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertTrue
import org.junit.Test

class MangadexSourceInterceptorTest {
    @Test
    fun intercept_deve_adicionar_User_Agent_customizado() {
        // Arrange
        val interceptor = MangadexInterceptor()
        val chain = mockk<Interceptor.Chain>()
        val request =
            Request
                .Builder()
                .url("https://api.mangadex.org/manga")
                .build()

        val requestSlot = slot<Request>()

        every { chain.request() } returns request
        every { chain.proceed(capture(requestSlot)) } returns mockk<Response>()

        // Act
        interceptor.intercept(chain)

        // Assert
        val capturedRequest = requestSlot.captured
        val userAgent = capturedRequest.header("User-Agent")

        assert(userAgent != null)
        assertTrue("User-Agent deve conter AcerolaMangaApp", userAgent!!.contains("AcerolaMangaApp"))
    }
}
