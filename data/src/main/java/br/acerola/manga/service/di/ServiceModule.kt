package br.acerola.manga.service.di

import br.acerola.manga.service.compact.CbzCompactService
import br.acerola.manga.service.compact.ArchiveCompactService
import br.acerola.manga.service.download.MangadexChapterDownloadService
import br.acerola.manga.service.download.ChapterDownloadService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindArchiveCompactService(
        impl: CbzCompactService
    ): ArchiveCompactService

    @Binds
    @Singleton
    abstract fun bindChapterDownloadService(
        impl: MangadexChapterDownloadService
    ): ChapterDownloadService
}
