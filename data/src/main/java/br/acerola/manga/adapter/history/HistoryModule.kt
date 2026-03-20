package br.acerola.manga.adapter.history

import br.acerola.manga.adapter.contract.HistoryPort
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
    @LocalHistoryEngine
    abstract fun bindHistoryRepository(
        impl: LocalHistoryEgine
    ): HistoryPort
}
