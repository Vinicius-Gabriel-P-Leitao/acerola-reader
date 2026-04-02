package br.acerola.manga.adapter.metadata.anilist

import br.acerola.manga.adapter.contract.provider.ImageProvider
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.adapter.metadata.anilist.engine.AnilistMangaEngine
import br.acerola.manga.adapter.metadata.anilist.source.AnilistFetchCoverSource
import br.acerola.manga.adapter.metadata.anilist.source.AnilistMangaInfoSource
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
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
    ): MangaGateway<MangaMetadataDto>

    @Binds @Singleton @AnilistSource
    abstract fun bindAnilistMangaInfoSource(
        impl: AnilistMangaInfoSource
    ): MetadataProvider<MangaMetadataDto, String>

    @Binds @Singleton @AnilistSource
    abstract fun bindAnilistFetchCoverSource(
        impl: AnilistFetchCoverSource
    ): ImageProvider<String>
}
