package br.acerola.manga.adapter.di

import br.acerola.manga.adapter.impl.history.LocalHistoryEngine
import br.acerola.manga.adapter.port.HistoryPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class LocalHistoryEngine

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {

    @Binds
    @Singleton
    @br.acerola.manga.adapter.di.LocalHistoryEngine
    abstract fun bindHistoryRepository(
        impl: LocalHistoryEngine
    ): HistoryPort
}
