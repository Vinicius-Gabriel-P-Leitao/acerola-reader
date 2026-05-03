package br.acerola.comic.usecase

import br.acerola.comic.adapter.contract.gateway.ChapterSyncGateway
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.adapter.metadata.anilist.AnilistEngine
import br.acerola.comic.adapter.metadata.comicinfo.ComicInfoEngine
import br.acerola.comic.adapter.metadata.mangadex.MangadexEngine
import br.acerola.comic.dto.view.ComicSummaryDto
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.library.RescanComicChaptersUseCase
import br.acerola.comic.usecase.library.RescanComicUseCase
import br.acerola.comic.usecase.library.SyncLibraryUseCase
import br.acerola.comic.usecase.metadata.SyncComicMetadataUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class ComicInfoCase

@Module
@InstallIn(SingletonComponent::class)
object ComicInfoCaseModule {
    @Provides
    @ComicInfoCase
    fun provideSyncLibraryUseCase(
        @ComicInfoEngine scanGateway: ComicLibraryScanGateway,
        @ComicInfoEngine chapterGateway: ChapterSyncGateway,
    ): SyncLibraryUseCase = SyncLibraryUseCase(
        scanGateway = scanGateway,
        chapterGateway = chapterGateway,
    )

    @Provides
    @ComicInfoCase
    fun provideObserveLibraryUseCase(
        @br.acerola.comic.adapter.library.SummaryEngine summaryRepo: ComicReadOnlyGateway<ComicSummaryDto>,
        @ComicInfoEngine syncOps: ComicSingleSyncGateway,
    ): ObserveLibraryUseCase<ComicSummaryDto> =
        ObserveLibraryUseCase(
            comicRepository = summaryRepo,
            syncGateway = syncOps,
        )

    @Provides
    @ComicInfoCase
    fun provideRescanComicUseCase(
        @ComicInfoEngine syncOps: ComicSingleSyncGateway,
    ): RescanComicUseCase =
        RescanComicUseCase(
            comicRepository = syncOps,
        )

    @Provides
    @ComicInfoCase
    fun provideRescanComicChaptersUseCase(
        @ComicInfoEngine chapterOps: ChapterSyncGateway,
    ): RescanComicChaptersUseCase =
        RescanComicChaptersUseCase(
            chapterRepository = chapterOps,
        )

    @Provides
    fun provideSyncComicMetadataUseCase(
        @AnilistEngine anilistMangaRepo: ComicSingleSyncGateway,
        @MangadexEngine mangadexMangaRepo: ComicSingleSyncGateway,
        @MangadexEngine mangadexChapterRepo: ChapterSyncGateway,
        @ComicInfoEngine comicInfoMangaRepo: ComicSingleSyncGateway,
        @ComicInfoEngine comicInfoChapterRepo: ChapterSyncGateway,
    ): SyncComicMetadataUseCase =
        SyncComicMetadataUseCase(
            anilistMangaRepo,
            mangadexMangaRepo,
            mangadexChapterRepo,
            comicInfoMangaRepo,
            comicInfoChapterRepo,
        )
}
