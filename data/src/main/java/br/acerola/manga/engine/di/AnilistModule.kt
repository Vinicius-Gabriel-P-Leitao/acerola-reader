package br.acerola.manga.engine.di

import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.engine.adapter.anilist.AnilistMangaAdapter
import br.acerola.manga.engine.port.MangaPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class AnilistFsOps

@Module
@InstallIn(SingletonComponent::class)
abstract class AnilistModule {

    @Binds
    @Singleton
    @AnilistFsOps
    abstract fun bindAnilistMangaRepository(
        impl: AnilistMangaAdapter
    ): MangaPort<MangaRemoteInfoDto>
}