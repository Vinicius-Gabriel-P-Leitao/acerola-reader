package br.acerola.comic.usecase

import br.acerola.comic.service.P2pService
import br.acerola.comic.usecase.network.P2pUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkCaseModule {
    @Provides
    @Singleton
    fun provideP2pService(): P2pService =
        P2pService { event, data ->
        }

    @Provides
    @Singleton
    fun provideP2pUseCase(p2pService: P2pService): P2pUseCase = P2pUseCase(p2pService)
}
