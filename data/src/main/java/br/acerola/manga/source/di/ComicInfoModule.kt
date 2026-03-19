package br.acerola.manga.source.di

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.source.adapter.comicInfo.ChapterComicInfoPort
import br.acerola.manga.source.adapter.comicInfo.MangaComicInfoPort
import br.acerola.manga.source.port.RemoteInfoOperationsPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class ComicInfo

@Module
@InstallIn(SingletonComponent::class)
abstract class ComicInfoModule {

    @Binds
    @Singleton
    @ComicInfo
    abstract fun bindComicInfoMangaInfoService(
        impl: MangaComicInfoPort
    ): RemoteInfoOperationsPort<MangaRemoteInfoDto, String>

    @Binds
    @Singleton
    @ComicInfo
    abstract fun bindComicInfoChapterInfoService(
        impl: ChapterComicInfoPort
    ): RemoteInfoOperationsPort<ChapterRemoteInfoDto, String>
}