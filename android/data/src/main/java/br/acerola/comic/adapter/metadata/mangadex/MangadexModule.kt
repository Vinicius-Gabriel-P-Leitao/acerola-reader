package br.acerola.comic.adapter.metadata.mangadex

import br.acerola.comic.adapter.contract.gateway.ChapterReadGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.adapter.contract.provider.ImageProvider
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.adapter.metadata.mangadex.engine.MangadexChapterEngine
import br.acerola.comic.adapter.metadata.mangadex.engine.MangadexComicEngine
import br.acerola.comic.adapter.metadata.mangadex.source.MangadexChapterInfoSource
import br.acerola.comic.adapter.metadata.mangadex.source.MangadexFetchCoverSource
import br.acerola.comic.adapter.metadata.mangadex.source.MangadexMangaInfoSource
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
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
    abstract fun bindMangadexSingleSync(impl: MangadexComicEngine): ComicSingleSyncGateway

    @Binds
    @Singleton
    @MangadexEngine
    abstract fun bindMangadexLibraryScan(impl: MangadexComicEngine): ComicLibraryScanGateway

    @Binds
    @Singleton
    @MangadexEngine
    abstract fun bindMangadexReadOnly(impl: MangadexComicEngine): ComicReadOnlyGateway<ComicMetadataDto>

    @Binds
    @Singleton
    @MangadexEngine
    abstract fun bindMangadexChapterSyncRepository(impl: MangadexChapterEngine): ChapterSyncGateway

    @Binds
    @Singleton
    @MangadexEngine
    abstract fun bindMangadexChapterStatusRepository(impl: MangadexChapterEngine): ChapterSyncStatusGateway

    @Binds
    @Singleton
    @MangadexEngine
    abstract fun bindMangadexChapterReadRepository(impl: MangadexChapterEngine): ChapterReadGateway<ChapterRemoteInfoPageDto>

    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexMangaInfoService(impl: MangadexMangaInfoSource): MetadataProvider<ComicMetadataDto, String>

    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexFetchCoverService(impl: MangadexFetchCoverSource): ImageProvider<String>

    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexChapterInfoService(impl: MangadexChapterInfoSource): MetadataProvider<ChapterMetadataDto, String>
}
