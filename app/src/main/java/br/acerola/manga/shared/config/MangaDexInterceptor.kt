package br.acerola.manga.shared.config


import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class MangaDexInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val newRequest = originalRequest.newBuilder()
            .header("User-Agent", "AcerolaMangaApp/1.0 (vinicius.gabriel.p.leitao@proton.me)")
            .build()

        var response = chain.proceed(newRequest)
        var tryCount = 0
        val maxRetries = 3

        while (!response.isSuccessful && response.code == 429 && tryCount < maxRetries) {
            response.close()

            val retryAfter = response.header(name = "X-RateLimit-Retry-After")
                ?.toLongOrNull()
                ?: response.header(name = "Retry-After")?.toLongOrNull()
                ?: 1L

            try {
                TimeUnit.SECONDS.sleep(retryAfter + 1)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            tryCount++
            response = chain.proceed(newRequest)
        }

        return response
    }
}