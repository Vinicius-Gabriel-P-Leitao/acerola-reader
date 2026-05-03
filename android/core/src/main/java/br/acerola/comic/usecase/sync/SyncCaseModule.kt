package br.acerola.comic.usecase.sync

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncCaseModule {
    @Provides
    @Singleton
    fun provideSyncLibraryUseCase(
        useCase: SyncLibraryUseCase
    ): SyncLibraryUseCase = useCase

    @Provides
    @Singleton
    fun provideSyncMetadataUseCase(
        useCase: SyncMetadataUseCase
    ): SyncMetadataUseCase = useCase
}
