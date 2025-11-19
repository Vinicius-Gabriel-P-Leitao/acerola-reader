package br.acerola.manga.shared.config

import br.acerola.manga.BuildConfig
import br.acerola.manga.domain.database.dao.api.mangadex.manga.MangaDataMangaDexDao
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object MangaDexApiModule {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeout = 30, unit = TimeUnit.SECONDS)
        .readTimeout(timeout = 30, unit = TimeUnit.SECONDS)
        .build()

    val mangaDataMangadexDao: MangaDataMangaDexDao = Retrofit.Builder()
        .baseUrl(BuildConfig.MANGADEX_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MangaDataMangaDexDao::class.java)
}