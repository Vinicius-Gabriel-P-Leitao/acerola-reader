package br.acerola.manga.remote.mangadex

import br.acerola.manga.data.BuildConfig
import br.acerola.manga.remote.mangadex.api.MangadexChapterMetadataClient
import br.acerola.manga.remote.mangadex.api.MangadexMangaDownloadClient
import br.acerola.manga.remote.mangadex.api.MangadexMangaMetadataClient
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
    fun provideMangadexMangaInfoApi(@MainApi retrofit: Retrofit): MangadexMangaMetadataClient {
        return retrofit.create(MangadexMangaMetadataClient::class.java)
    }

    @Provides
    @Singleton
    fun provideMangadexDownloadApi(@DownloadApi retrofit: Retrofit): MangadexMangaDownloadClient {
        return retrofit.create(MangadexMangaDownloadClient::class.java)
    }

    @Provides
    @Singleton
    fun provideMangadexChapterInfoApi(@MainApi retrofit: Retrofit): MangadexChapterMetadataClient {
        return retrofit.create(MangadexChapterMetadataClient::class.java)
    }

}