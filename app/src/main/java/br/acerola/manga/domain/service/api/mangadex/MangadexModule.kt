package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.BuildConfig
import br.acerola.manga.domain.data.dao.api.mangadex.cover.MangaDexDownloadDao
import br.acerola.manga.domain.data.dao.api.mangadex.manga.MangaDataMangaDexDao
import br.acerola.manga.domain.middleware.MangaDexInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class DownloadApi

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class MainApi

@Module
@InstallIn(SingletonComponent::class)
object MangadexModule {

    @MainApi
    @Provides
    @Singleton
    fun provideMainOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(interceptor = MangaDexInterceptor())
            .connectTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .writeTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .readTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .build()
    }

    // NOTE: Retorna em json
    @MainApi
    @Provides
    @Singleton
    fun provideMainRetrofit(@MainApi okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.MANGADEX_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMangaDataMangaDexDao(@MainApi retrofit: Retrofit): MangaDataMangaDexDao {
        return retrofit.create(MangaDataMangaDexDao::class.java)
    }

    // NOTE: Injeçao de dependencia para dados blob
    @Provides
    @Singleton
    @DownloadApi
    fun provideDownloadRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.MANGADEX_UPLOAD_URL)
            .build()
    }

    @Provides
    @Singleton
    fun provideMangaDexDownloadDao(@DownloadApi retrofit: Retrofit): MangaDexDownloadDao {
        return retrofit.create(MangaDexDownloadDao::class.java)
    }
}