package br.acerola.manga.domain.service.api

import br.acerola.manga.domain.service.api.mangadex.MangadexFetchChapterDataService
import br.acerola.manga.domain.service.api.mangadex.MangadexFetchCoverService
import br.acerola.manga.domain.service.api.mangadex.MangadexFetchMangaDataService
import br.acerola.manga.shared.dto.metadata.ChapterMetadataDto
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
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
    abstract fun bindMangaMetadataOperation(
        impl: MangadexFetchMangaDataService
    ): ApiPort.MetadataOperations<MangaMetadataDto, String>

    @Binds
    @Singleton
    abstract fun bindMangadexFetchChapterOperation(
        impl: MangadexFetchChapterDataService
    ): ApiPort.MetadataOperations<ChapterMetadataDto, String>

    @Binds
    @Singleton
    abstract fun bindMangaDexFetchMangaDataService(
        impl: MangadexFetchCoverService
    ): ApiPort.ArchiveOperations<String>
}