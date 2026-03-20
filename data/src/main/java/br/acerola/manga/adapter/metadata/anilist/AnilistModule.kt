package br.acerola.manga.adapter.metadata.anilist

import br.acerola.manga.adapter.contract.ImageFetchPort
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.adapter.contract.RemoteInfoOperationsPort
import br.acerola.manga.adapter.metadata.anilist.engine.AnilistMangaEngine
import br.acerola.manga.adapter.metadata.anilist.source.AnilistFetchCoverSource
import br.acerola.manga.adapter.metadata.anilist.source.AnilistMangaInfoSource
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnilistSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnilistEngine

@Module
@InstallIn(SingletonComponent::class)
abstract class AnilistModule {

    @Binds @Singleton @AnilistEngine
    abstract fun bindAnilistMangaEngine(
        impl: AnilistMangaEngine
    ): MangaPort<MangaRemoteInfoDto>

    @Binds @Singleton @AnilistSource
    abstract fun bindAnilistMangaInfoSource(
        impl: AnilistMangaInfoSource
    ): RemoteInfoOperationsPort<MangaRemoteInfoDto, String>

    @Binds @Singleton @AnilistSource
    abstract fun bindAnilistFetchCoverSource(
        impl: AnilistFetchCoverSource
    ): ImageFetchPort<String>
}
