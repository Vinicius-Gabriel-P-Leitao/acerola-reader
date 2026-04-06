package br.acerola.comic.remote.mangadex.interceptor

import br.acerola.comic.data.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class MangadexInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header(name = "User-Agent", value = "AcerolaMangaApp/1.0 (${BuildConfig.GITHUB_USER_AGENT})")
            .build()

        return chain.proceed(newRequest)
    }
}