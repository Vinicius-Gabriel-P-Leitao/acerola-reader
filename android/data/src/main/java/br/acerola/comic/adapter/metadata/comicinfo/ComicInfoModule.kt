package br.acerola.comic.adapter.metadata.comicinfo

import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.adapter.metadata.comicinfo.engine.ComicInfoChapterEngine
import br.acerola.comic.adapter.metadata.comicinfo.engine.ComicInfoComicEngine
import br.acerola.comic.adapter.metadata.comicinfo.source.ChapterComicInfoSource
import br.acerola.comic.adapter.metadata.comicinfo.source.ComicInfoSource
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
annotation class ComicInfoEngine

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class ComicInfoSourceQualifier

@Module
@InstallIn(SingletonComponent::class)
abstract class ComicInfoModule {

    @Binds
    @Singleton
    @ComicInfoEngine
    abstract fun bindComicInfoMangaRepository(
        impl: ComicInfoComicEngine
    ): br.acerola.comic.adapter.contract.gateway.ComicSyncGateway

    @Binds
    @Singleton
    @ComicInfoEngine
    abstract fun bindComicInfoChapterRepository(
        impl: ComicInfoChapterEngine
    ): ChapterGateway<ChapterRemoteInfoPageDto>

    @Binds
    @Singleton
    @ComicInfoSourceQualifier
    abstract fun bindComicInfoMangaInfoService(
        impl: ComicInfoSource
    ): MetadataProvider<ComicMetadataDto, String>

    @Binds
    @Singleton
    @ComicInfoSourceQualifier
    abstract fun bindComicInfoChapterInfoService(
        impl: ChapterComicInfoSource
    ): MetadataProvider<ChapterMetadataDto, String>
}
