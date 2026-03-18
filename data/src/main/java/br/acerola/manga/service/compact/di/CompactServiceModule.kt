package br.acerola.manga.service.compact.di

import br.acerola.manga.service.compact.ArchiveCompactService
import br.acerola.manga.service.compact.CbzCompactService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CompactServiceModule {

    @Binds
    @Singleton
    abstract fun bindArchiveCompactService(
        impl: CbzCompactService
    ): ArchiveCompactService
}
