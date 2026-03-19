package br.acerola.manga.engine.di

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.engine.adapter.mangadex.MangadexChapterAdapter
import br.acerola.manga.engine.adapter.mangadex.MangadexMangaAdapter
import br.acerola.manga.engine.port.AnilistLinkRepository
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
annotation class MangadexFsOps

@Module
@InstallIn(SingletonComponent::class)
abstract class MangadexModule {

    @Binds
    @Singleton
    @MangadexFsOps
    abstract fun bindMangadexMangaRepository(
        impl: MangadexMangaAdapter
    ): MangaPort<MangaRemoteInfoDto>

    @Binds
    @Singleton
    @MangadexFsOps
    abstract fun bindMangadexChapterRepository(
        impl: MangadexChapterAdapter
    ): ChapterPort<ChapterRemoteInfoPageDto>


    @Binds
    @Singleton
    @MangadexFsOps
    abstract fun bindAnilistLinkRepository(
        impl: MangadexMangaAdapter
    ): AnilistLinkRepository
}