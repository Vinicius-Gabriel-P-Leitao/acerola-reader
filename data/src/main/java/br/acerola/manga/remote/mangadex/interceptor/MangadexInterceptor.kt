package br.acerola.manga.remote.mangadex.interceptor

import br.acerola.manga.data.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class MangadexInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val newRequest = originalRequest
            .newBuilder()
            .header(name = "User-Agent", value = "AcerolaMangaApp/1.0 (${BuildConfig.GITHUB_USER_AGENT})")
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
                // TODO: Tratar erro melhor
            } catch (interruptedException: InterruptedException) {
                interruptedException.printStackTrace()
            }

            tryCount++
            response = chain.proceed(newRequest)
        }

        return response
    }
}