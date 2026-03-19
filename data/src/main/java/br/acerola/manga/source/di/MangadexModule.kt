package br.acerola.manga.source.di

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.service.download.ChapterDownloadService
import br.acerola.manga.service.download.MangadexChapterDownloadService
import br.acerola.manga.source.adapter.mangadex.chapter.MangadexChapterInfoPort
import br.acerola.manga.source.adapter.mangadex.download.MangadexSearchDownloadPort
import br.acerola.manga.source.adapter.mangadex.manga.MangadexFetchCoverPort
import br.acerola.manga.source.adapter.mangadex.manga.MangadexMangaInfoPort
import br.acerola.manga.source.port.BinaryOperationsPort
import br.acerola.manga.source.port.DownloadPort
import br.acerola.manga.source.port.RemoteInfoOperationsPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class Mangadex

@Module
@InstallIn(SingletonComponent::class)
abstract class MangadexModule {
    @Binds
    @Mangadex
    @Singleton
    abstract fun bindMangadexMangaInfoService(
        impl: MangadexMangaInfoPort
    ): RemoteInfoOperationsPort<MangaRemoteInfoDto, String>

    @Binds
    @Mangadex
    @Singleton
    abstract fun bindMangadexChapterInfoService(
        impl: MangadexChapterInfoPort
    ): RemoteInfoOperationsPort<ChapterRemoteInfoDto, String>

    @Binds
    @Mangadex
    @Singleton
    abstract fun bindMangadexFetchCoverService(
        impl: MangadexFetchCoverPort
    ): BinaryOperationsPort<String>

    @Binds
    @Mangadex
    @Singleton
    abstract fun bindDownloadRepository(
        impl: MangadexSearchDownloadPort
    ): DownloadPort

    @Binds
    @Mangadex
    @Singleton
    abstract fun bindChapterDownloadService(
        impl: MangadexChapterDownloadService
    ): ChapterDownloadService
}