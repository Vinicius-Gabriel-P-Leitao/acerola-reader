package br.acerola.manga.engine.di

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.engine.adapter.comicInfo.ComicInfoChapterAdapter
import br.acerola.manga.engine.adapter.comicInfo.ComicInfoMangaAdapter
import br.acerola.manga.engine.port.ChapterPort
import br.acerola.manga.engine.port.MangaPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class ComicInfoFsOps

@Module
@InstallIn(SingletonComponent::class)
abstract class ComicInfoModule {

    @Binds
    @Singleton
    @ComicInfoFsOps
    abstract fun bindComicInfoMangaRepository(
        impl: ComicInfoMangaAdapter
    ): MangaPort<MangaRemoteInfoDto>

    @Binds
    @Singleton
    @ComicInfoFsOps
    abstract fun bindComicInfoChapterRepository(
        impl: ComicInfoChapterAdapter
    ): ChapterPort<ChapterRemoteInfoPageDto>
}