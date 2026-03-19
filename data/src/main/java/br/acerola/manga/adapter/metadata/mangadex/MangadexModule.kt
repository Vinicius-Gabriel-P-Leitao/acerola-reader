package br.acerola.manga.adapter.metadata.mangadex

import br.acerola.manga.adapter.contract.ChapterPort
import br.acerola.manga.adapter.contract.DownloadPort
import br.acerola.manga.adapter.contract.ImageFetchPort
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.adapter.contract.RemoteInfoOperationsPort
import br.acerola.manga.adapter.metadata.mangadex.engine.MangadexChapterEngine
import br.acerola.manga.adapter.metadata.mangadex.engine.MangadexMangaEngine
import br.acerola.manga.adapter.metadata.mangadex.source.MangadexChapterInfoSource
import br.acerola.manga.adapter.metadata.mangadex.source.MangadexFetchCoverSource
import br.acerola.manga.adapter.metadata.mangadex.source.MangadexMangaInfoSource
import br.acerola.manga.adapter.metadata.mangadex.source.MangadexSearchDownloadSource
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.service.download.ChapterDownloadService
import br.acerola.manga.service.download.MangadexChapterDownloadService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class MangadexEngine

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class MangadexSource

@Module
@InstallIn(SingletonComponent::class)
abstract class MangadexModule {

    @Binds
    @Singleton
    @MangadexEngine
    abstract fun bindMangadexMangaRepository(
        impl: MangadexMangaEngine
    ): MangaPort<MangaRemoteInfoDto>

    @Binds
    @Singleton
    @MangadexEngine
    abstract fun bindMangadexChapterRepository(
        impl: MangadexChapterEngine
    ): ChapterPort<ChapterRemoteInfoPageDto>


    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexMangaInfoService(
        impl: MangadexMangaInfoSource
    ): RemoteInfoOperationsPort<MangaRemoteInfoDto, String>

    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexFetchCoverService(
        impl: MangadexFetchCoverSource
    ): ImageFetchPort<String>

    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindChapterDownloadService(
        impl: MangadexChapterDownloadService
    ): ChapterDownloadService

    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexChapterInfoService(
        impl: MangadexChapterInfoSource
    ): RemoteInfoOperationsPort<ChapterRemoteInfoDto, String>

    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexDownloadPort(
        impl: MangadexSearchDownloadSource
    ): DownloadPort
}
