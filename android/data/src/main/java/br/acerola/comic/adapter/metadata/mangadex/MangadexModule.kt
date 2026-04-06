package br.acerola.comic.adapter.metadata.mangadex

import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.provider.ImageProvider
import br.acerola.comic.adapter.contract.gateway.ComicGateway
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
    abstract fun bindMangadexMangaRepository(
        impl: MangadexComicEngine
    ): ComicGateway<ComicMetadataDto>

    @Binds
    @Singleton
    @MangadexEngine
    abstract fun bindMangadexChapterRepository(
        impl: MangadexChapterEngine
    ): ChapterGateway<ChapterRemoteInfoPageDto>


    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexMangaInfoService(
        impl: MangadexMangaInfoSource
    ): MetadataProvider<ComicMetadataDto, String>

    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexFetchCoverService(
        impl: MangadexFetchCoverSource
    ): ImageProvider<String>

    @Binds
    @Singleton
    @MangadexSource
    abstract fun bindMangadexChapterInfoService(
        impl: MangadexChapterInfoSource
    ): MetadataProvider<ChapterMetadataDto, String>
}
