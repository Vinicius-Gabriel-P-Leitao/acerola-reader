package br.acerola.manga.repository.adapter.remote.mangadex

import br.acerola.manga.data.BuildConfig
import br.acerola.manga.remote.mangadex.api.MangadexChapterInfoApi
import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import br.acerola.manga.remote.mangadex.api.MangadexMangaInfoApi
import br.acerola.manga.remote.mangadex.interceptor.MangadexInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class MainApi

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class DownloadApi

@Module
@InstallIn(SingletonComponent::class)
object MangadexModule {

    @MainApi
    @Provides
    @Singleton
    fun provideMainOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(interceptor = MangadexInterceptor())
            .connectTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .writeTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .readTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .build()
    }

    @MainApi
    @Provides
    @Singleton
    fun provideMainRetrofit(@MainApi okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.MANGADEX_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @DownloadApi
    fun provideDownloadRetrofit(): Retrofit {
        return Retrofit.Builder().baseUrl(BuildConfig.MANGADEX_UPLOAD_URL).build()
    }

    @Provides
    @Singleton
    fun provideMangadexMangaInfoApi(@MainApi retrofit: Retrofit): MangadexMangaInfoApi {
        return retrofit.create(MangadexMangaInfoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMangadexDownloadApi(@DownloadApi retrofit: Retrofit): MangadexDownloadApi {
        return retrofit.create(MangadexDownloadApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMangadexChapterInfoApi(@MainApi retrofit: Retrofit): MangadexChapterInfoApi {
        return retrofit.create(MangadexChapterInfoApi::class.java)
    }
}