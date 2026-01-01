package br.acerola.manga.repository.port

import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.repository.adapter.remote.mangadex.chapter.MangadexMetadataChapterService
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexFetchCoverService
import br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexMetadataMangaService
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
    abstract fun bindMangaMetadataOperation(
        impl: MangadexMetadataMangaService
    ): ApiRepository.MetadataOperations<MangaMetadataDto, String>

    @Binds
    @Singleton
    abstract fun bindMangadexFetchChapterOperation(
        impl: MangadexMetadataChapterService
    ): ApiRepository.MetadataOperations<ChapterMetadataDto, String>

    @Binds
    @Singleton
    abstract fun bindMangaDexFetchMangaDataService(
        impl: MangadexFetchCoverService
    ): ApiRepository.ArchiveOperations<String>
}