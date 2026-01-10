package br.acerola.manga.repository.di

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.adapter.remote.mangadex.chapter.MangadexChapterInfoService
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexFetchCoverService
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexMangaInfoService
import br.acerola.manga.repository.port.RemoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class Mangadex

/**
 * Esse modulo é para os serviços de busca de dados, só API, eles pegam dados tratam e retornam um DTO, tem que ser
 * usado por outros services, não são feitos para serem usados em ViewModel.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {
    @Binds
    @Singleton
    @Mangadex
    abstract fun bindMangadexMangaInfoService(
        impl: MangadexMangaInfoService
    ): RemoteRepository.RemoteInfoOperations<MangaRemoteInfoDto, String>

    @Binds
    @Singleton
    @Mangadex
    abstract fun bindMangadexChapterInfoService(
        impl: MangadexChapterInfoService
    ): RemoteRepository.RemoteInfoOperations<ChapterRemoteInfoDto, String>

    @Binds
    @Singleton
    @Mangadex
    abstract fun bindMangadexFetchCoverService(
        impl: MangadexFetchCoverService
    ): RemoteRepository.ArchiveOperations<String>
}