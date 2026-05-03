package br.acerola.comic.usecase

import br.acerola.comic.adapter.contract.gateway.ChapterReadGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.adapter.metadata.mangadex.MangadexEngine
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.library.RescanComicChaptersUseCase
import br.acerola.comic.usecase.library.RescanComicUseCase
import br.acerola.comic.usecase.library.SyncLibraryUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class MangadexCase

@Module
@InstallIn(SingletonComponent::class)
object MangadexCaseModule {
    @Provides
    @MangadexCase
    fun provideSyncLibraryUseCase(
        @MangadexEngine scanGateway: ComicLibraryScanGateway,
    ): SyncLibraryUseCase = SyncLibraryUseCase(scanGateway = scanGateway)

    @Provides
    @MangadexCase
    fun provideObserveLibraryUseCase(
        @MangadexEngine repository: ComicReadOnlyGateway<ComicMetadataDto>,
        @MangadexEngine syncStatus: ComicSingleSyncGateway,
    ): ObserveLibraryUseCase<ComicMetadataDto> =
        ObserveLibraryUseCase(
            comicRepository = repository,
            syncGateway = syncStatus,
        )

    @Provides
    @MangadexCase
    fun provideRescanComicUseCase(
        @MangadexEngine comicOps: ComicSingleSyncGateway,
    ): RescanComicUseCase =
        RescanComicUseCase(
            comicRepository = comicOps,
        )

    @Provides
    @MangadexCase
    fun provideRescanComicChaptersUseCase(
        @MangadexEngine chapterOps: ChapterSyncGateway,
    ): RescanComicChaptersUseCase =
        RescanComicChaptersUseCase(
            chapterRepository = chapterOps,
        )

    @Provides
    @MangadexCase
    fun provideGetChaptersUseCase(
        @MangadexEngine readOps: ChapterReadGateway<ChapterRemoteInfoPageDto>,
        @MangadexEngine statusOps: ChapterSyncStatusGateway,
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> =
        ObserveChaptersUseCase(
            readGateway = readOps,
            syncStatusGateway = statusOps,
        )
}
