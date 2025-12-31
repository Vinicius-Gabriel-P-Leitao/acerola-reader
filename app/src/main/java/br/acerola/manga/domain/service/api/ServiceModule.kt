package br.acerola.manga.domain.service.api

import br.acerola.manga.domain.service.api.mangadex.MangadexFetchChapterDataService
import br.acerola.manga.domain.service.api.mangadex.MangadexFetchCoverService
import br.acerola.manga.domain.service.api.mangadex.MangadexFetchMangaDataService
import br.acerola.manga.domain.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.domain.dto.metadata.manga.MangaMetadataDto
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
    ): MangaRepository.MetadataOperations<MangaMetadataDto, String>

    @Binds
    @Singleton
    abstract fun bindMangadexFetchChapterOperation(
        impl: MangadexFetchChapterDataService
    ): MangaRepository.MetadataOperations<ChapterMetadataDto, String>

    @Binds
    @Singleton
    abstract fun bindMangaDexFetchMangaDataService(
        impl: MangadexFetchCoverService
    ): MangaRepository.ArchiveOperations<String>
}