package br.acerola.manga.domain.service.api

import br.acerola.manga.domain.service.api.mangadex.MangaDexFetchCoverService
import br.acerola.manga.domain.service.api.mangadex.MangaDexFetchMangaDataService
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
        impl: MangaDexFetchMangaDataService
    ): ApiPort.MetadataOperations<MangaMetadataDto, String>

    @Binds
    @Singleton
    abstract fun bindMangaDexFetchMangaDataService(
        impl: MangaDexFetchCoverService
    ): ApiPort.ArchiveOperations<String>

}