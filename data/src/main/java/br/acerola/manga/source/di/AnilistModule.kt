package br.acerola.manga.source.di

import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.source.adapter.anilist.AnilistFetchCoverPort
import br.acerola.manga.source.adapter.anilist.AnilistMangaInfoPort
import br.acerola.manga.source.port.BinaryOperationsPort
import br.acerola.manga.source.port.RemoteInfoOperationsPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class Anilist

@Module
@InstallIn(SingletonComponent::class)
abstract class AnilistModule {
    @Binds
    @Singleton
    @Anilist
    abstract fun bindAnilistMangaInfoService(
        impl: AnilistMangaInfoPort
    ): RemoteInfoOperationsPort<MangaRemoteInfoDto, String>

    @Binds
    @Singleton
    @Anilist
    abstract fun bindAnilistFetchCoverService(
        impl: AnilistFetchCoverPort
    ): BinaryOperationsPort<String>
}