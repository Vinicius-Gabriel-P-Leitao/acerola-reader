package br.acerola.manga.adapter.metadata.comicinfo

import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.adapter.metadata.comicinfo.engine.ComicInfoChapterEngine
import br.acerola.manga.adapter.metadata.comicinfo.engine.ComicInfoMangaEngine
import br.acerola.manga.adapter.metadata.comicinfo.source.ChapterComicInfoSource
import br.acerola.manga.adapter.metadata.comicinfo.source.MangaComicInfoSource
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
annotation class ComicInfoEngine

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class ComicInfoSource

@Module
@InstallIn(SingletonComponent::class)
abstract class ComicInfoModule {

    @Binds
    @Singleton
    @ComicInfoEngine
    abstract fun bindComicInfoMangaRepository(
        impl: ComicInfoMangaEngine
    ): br.acerola.manga.adapter.contract.gateway.MangaSyncGateway

    @Binds
    @Singleton
    @ComicInfoEngine
    abstract fun bindComicInfoChapterRepository(
        impl: ComicInfoChapterEngine
    ): ChapterGateway<ChapterRemoteInfoPageDto>

    @Binds
    @Singleton
    @ComicInfoSource
    abstract fun bindComicInfoMangaInfoService(
        impl: MangaComicInfoSource
    ): MetadataProvider<MangaMetadataDto, String>

    @Binds
    @Singleton
    @ComicInfoSource
    abstract fun bindComicInfoChapterInfoService(
        impl: ChapterComicInfoSource
    ): MetadataProvider<ChapterMetadataDto, String>
}
