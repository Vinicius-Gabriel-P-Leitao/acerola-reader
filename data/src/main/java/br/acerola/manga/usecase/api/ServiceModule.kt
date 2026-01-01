package br.acerola.manga.usecase.api

import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.usecase.api.mangadex.MangadexFetchChapterDataService
import br.acerola.manga.usecase.api.mangadex.MangadexFetchCoverService
import br.acerola.manga.usecase.api.mangadex.MangadexFetchMangaDataService
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