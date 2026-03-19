package br.acerola.manga.repository.di

import br.acerola.manga.repository.adapter.remote.mangadex.download.MangadexSearchDownloadRepository
import br.acerola.manga.repository.port.DownloadRepository
import br.acerola.manga.service.download.ChapterDownloadService
import br.acerola.manga.service.download.MangadexChapterDownloadService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadModule {

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: MangadexSearchDownloadRepository
    ): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindChapterDownloadService(
        impl: MangadexChapterDownloadService
    ): ChapterDownloadService
}
