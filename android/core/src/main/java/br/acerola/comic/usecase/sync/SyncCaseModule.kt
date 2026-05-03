package br.acerola.comic.usecase.sync

import br.acerola.comic.sync.LibrarySyncScheduler
import br.acerola.comic.sync.LibrarySyncStatusRepository
import br.acerola.comic.sync.WorkManagerLibrarySyncScheduler
import br.acerola.comic.sync.WorkManagerLibrarySyncStatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncCaseModule {

    @Binds
    @Singleton
    abstract fun bindLibrarySyncScheduler(
        scheduler: WorkManagerLibrarySyncScheduler
    ): LibrarySyncScheduler

    @Binds
    @Singleton
    abstract fun bindLibrarySyncStatusRepository(
        repository: WorkManagerLibrarySyncStatusRepository
    ): LibrarySyncStatusRepository
}
