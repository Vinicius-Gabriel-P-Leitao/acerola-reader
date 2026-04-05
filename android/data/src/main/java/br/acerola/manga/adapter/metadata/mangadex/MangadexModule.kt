package br.acerola.manga.adapter.metadata.mangadex

import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.provider.ImageProvider
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.adapter.metadata.mangadex.engine.MangadexChapterEngine
import br.acerola.manga.adapter.metadata.mangadex.engine.MangadexMangaEngine
import br.acerola.manga.adapter.metadata.mangadex.source.MangadexChapterInfoSource
import br.acerola.manga.adapter.metadata.mangadex.source.MangadexFetchCoverSource
import br.acerola.manga.adapter.metadata.mangadex.source.MangadexMangaInfoSource
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
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
    ): MangaGateway<MangaMetadataDto>

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
    ): MetadataProvider<MangaMetadataDto, String>

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
