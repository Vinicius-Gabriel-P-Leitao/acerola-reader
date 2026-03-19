package br.acerola.manga.engine.di

import br.acerola.manga.engine.adapter.history.LocalHistoryAdapter
import br.acerola.manga.engine.port.HistoryManagementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class LocalHistoryFsOps

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {

    @Binds
    @Singleton
    @LocalHistoryFsOps
    abstract fun bindHistoryRepository(
        impl: LocalHistoryAdapter
    ): HistoryManagementRepository
}
