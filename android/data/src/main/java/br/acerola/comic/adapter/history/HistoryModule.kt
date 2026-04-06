package br.acerola.comic.adapter.history

import br.acerola.comic.adapter.contract.gateway.HistoryGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class LocalHistory

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {

    @Binds
    @Singleton
    @LocalHistory
    abstract fun bindHistoryRepository(
        impl: LocalHistoryEngine
    ): HistoryGateway
}
