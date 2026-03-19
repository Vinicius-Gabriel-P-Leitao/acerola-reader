package br.acerola.manga.repository.di

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.adapter.remote.mangadex.chapter.MangadexChapterInfoRepository
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexFetchCoverRepository
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexMangaInfoRepository
import br.acerola.manga.repository.adapter.remote.xml.ChapterComicInfoRepository
import br.acerola.manga.repository.adapter.remote.xml.MangaComicInfoRepository
import br.acerola.manga.repository.port.BinaryOperationsRepository
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class Mangadex

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class ComicInfo

/**
 * Esse modulo é para os serviços de busca de dados, só API, eles pegam dados tratam e retornam um DTO, tem que ser
 * usado por outros services, não são feitos para serem usados em ViewModel.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {
    @Binds
    @Singleton
    @Mangadex
    abstract fun bindMangadexMangaInfoService(
        impl: MangadexMangaInfoRepository
    ): RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>

    @Binds
    @Singleton
    @Mangadex
    abstract fun bindMangadexChapterInfoService(
        impl: MangadexChapterInfoRepository
    ): RemoteInfoOperationsRepository<ChapterRemoteInfoDto, String>

    @Binds
    @Singleton
    @Mangadex
    abstract fun bindMangadexFetchCoverService(
        impl: MangadexFetchCoverRepository
    ): BinaryOperationsRepository<String>

    @Binds
    @Singleton
    @ComicInfo
    abstract fun bindComicInfoMangaInfoService(
        impl: MangaComicInfoRepository
    ): RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>

    @Binds
    @Singleton
    @ComicInfo
    abstract fun bindComicInfoChapterInfoService(
        impl: ChapterComicInfoRepository
    ): RemoteInfoOperationsRepository<ChapterRemoteInfoDto, String>
}