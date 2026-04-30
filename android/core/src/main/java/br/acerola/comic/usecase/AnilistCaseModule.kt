package br.acerola.comic.usecase

import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.metadata.anilist.AnilistEngine
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.library.RescanComicUseCase
import br.acerola.comic.usecase.library.SyncLibraryUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class AnilistCase

@Module
@InstallIn(SingletonComponent::class)
object AnilistCaseModule {
    @Provides
    @AnilistCase
    fun provideSyncLibraryUseCase(
        @AnilistEngine repository: ComicGateway<ComicMetadataDto>,
    ): SyncLibraryUseCase = SyncLibraryUseCase(repository)

    @Provides
    @AnilistCase
    fun provideObserveLibraryUseCase(
        @AnilistEngine repository: ComicGateway<ComicMetadataDto>,
    ): ObserveLibraryUseCase<ComicMetadataDto> =
        ObserveLibraryUseCase(
            comicRepository = repository,
            syncGateway = repository,
        )

    @Provides
    @AnilistCase
    fun provideRescanComicUseCase(
        @AnilistEngine repository: ComicGateway<ComicMetadataDto>,
    ): RescanComicUseCase =
        RescanComicUseCase(
            comicRepository = repository,
        )
}
