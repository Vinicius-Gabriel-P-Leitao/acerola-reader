package br.acerola.comic.adapter.metadata.anilist

import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.contract.provider.ImageProvider
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.adapter.metadata.anilist.engine.AnilistComicEngine
import br.acerola.comic.adapter.metadata.anilist.source.AnilistFetchCoverSource
import br.acerola.comic.adapter.metadata.anilist.source.AnilistMangaInfoSource
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
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
    abstract fun bindAnilistMangaEngine(impl: AnilistComicEngine): ComicGateway<ComicMetadataDto>

    @Binds @Singleton @AnilistSource
    abstract fun bindAnilistMangaInfoSource(impl: AnilistMangaInfoSource): MetadataProvider<ComicMetadataDto, String>

    @Binds @Singleton @AnilistSource
    abstract fun bindAnilistFetchCoverSource(impl: AnilistFetchCoverSource): ImageProvider<String>
}
