package br.acerola.manga.repository.port

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.adapter.remote.mangadex.chapter.MangadexChapterInfoService
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexFetchCoverService
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexMangaInfoService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {
    @Binds
    @Singleton
    abstract fun bindMangaRemoteInfoOperation(
        impl: MangadexMangaInfoService
    ): ApiRepository.RemoteInfoOperations<MangaRemoteInfoDto, String>

    @Binds
    @Singleton
    abstract fun bindMangadexFetchChapterOperation(
        impl: MangadexChapterInfoService
    ): ApiRepository.RemoteInfoOperations<ChapterRemoteInfoDto, String>

    @Binds
    @Singleton
    abstract fun bindMangaDexFetchMangaDataService(
        impl: MangadexFetchCoverService
    ): ApiRepository.ArchiveOperations<String>
}