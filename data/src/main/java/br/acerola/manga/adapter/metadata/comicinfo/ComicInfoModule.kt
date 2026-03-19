package br.acerola.manga.adapter.metadata.comicinfo

import br.acerola.manga.adapter.contract.ChapterPort
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.adapter.contract.RemoteInfoOperationsPort
import br.acerola.manga.adapter.metadata.comicinfo.engine.ComicInfoChapterEngine
import br.acerola.manga.adapter.metadata.comicinfo.engine.ComicInfoMangaEngine
import br.acerola.manga.adapter.metadata.comicinfo.source.ChapterComicInfoSource
import br.acerola.manga.adapter.metadata.comicinfo.source.MangaComicInfoSource
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
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
    ): MangaPort<MangaRemoteInfoDto>

    @Binds
    @Singleton
    @ComicInfoEngine
    abstract fun bindComicInfoChapterRepository(
        impl: ComicInfoChapterEngine
    ): ChapterPort<ChapterRemoteInfoPageDto>

    @Binds
    @Singleton
    @ComicInfoSource
    abstract fun bindComicInfoMangaInfoService(
        impl: MangaComicInfoSource
    ): RemoteInfoOperationsPort<MangaRemoteInfoDto, String>

    @Binds
    @Singleton
    @ComicInfoSource
    abstract fun bindComicInfoChapterInfoService(
        impl: ChapterComicInfoSource
    ): RemoteInfoOperationsPort<ChapterRemoteInfoDto, String>
}
