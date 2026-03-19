package br.acerola.manga.adapter.di

import br.acerola.manga.adapter.impl.mangadex.engine.MangadexChapterEngine
import br.acerola.manga.adapter.impl.mangadex.engine.MangadexMangaEngine
import br.acerola.manga.adapter.impl.mangadex.source.MangadexFetchCoverSource
import br.acerola.manga.adapter.impl.mangadex.source.MangadexMangaInfoSource
import br.acerola.manga.adapter.port.ImageFetchPort
import br.acerola.manga.adapter.port.ChapterPort
import br.acerola.manga.adapter.port.MangaPort
import br.acerola.manga.adapter.port.RemoteInfoOperationsPort
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
}