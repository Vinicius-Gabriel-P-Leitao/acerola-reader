package br.acerola.manga.core.usecase

import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.adapter.metadata.anilist.AnilistEngine
import br.acerola.manga.core.usecase.library.SyncLibraryUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
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
        @AnilistEngine repository: MangaPort<MangaRemoteInfoDto>
    ): SyncLibraryUseCase<MangaRemoteInfoDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @AnilistCase
    fun provideObserveLibraryUseCase(
        @AnilistEngine mangaOps: MangaPort<MangaRemoteInfoDto>
    ): ObserveLibraryUseCase<MangaRemoteInfoDto> {
        return ObserveLibraryUseCase(
            mangaRepository = mangaOps
        )
    }
}
