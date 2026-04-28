package br.acerola.comic.usecase

import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.adapter.contract.gateway.ComicSyncGateway
import br.acerola.comic.adapter.metadata.anilist.AnilistEngine
import br.acerola.comic.adapter.metadata.comicinfo.ComicInfoEngine
import br.acerola.comic.adapter.metadata.mangadex.MangadexEngine
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.dto.view.ComicSummaryDto
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
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
        @ComicInfoEngine repository: ComicSyncGateway,
    ): SyncLibraryUseCase = SyncLibraryUseCase(repository)

    @Provides
    @ComicInfoCase
    fun provideObserveLibraryUseCase(
        @br.acerola.comic.adapter.library.SummaryEngine summaryRepo: ComicReadOnlyGateway<br.acerola.comic.dto.view.ComicSummaryDto>,
        @ComicInfoEngine syncOps: ComicSyncGateway,
    ): ObserveLibraryUseCase<ComicSummaryDto> =
        ObserveLibraryUseCase(
            mangaRepository = summaryRepo,
            syncGateway = syncOps,
        )

    @Provides
    @ComicInfoCase
    fun provideRescanComicUseCase(
        @ComicInfoEngine syncOps: ComicSyncGateway,
    ): RescanComicUseCase =
        RescanComicUseCase(
            mangaRepository = syncOps,
        )

    @Provides
    @ComicInfoCase
    fun provideRescanComicChaptersUseCase(
        @ComicInfoEngine chapterOps: ChapterGateway<ChapterRemoteInfoPageDto>,
    ): RescanComicChaptersUseCase<ChapterRemoteInfoPageDto> =
        RescanComicChaptersUseCase(
            chapterRepository = chapterOps,
        )

    @Provides
    @ComicInfoCase
    fun provideGetChaptersUseCase(
        @ComicInfoEngine chapterOps: ChapterGateway<ChapterRemoteInfoPageDto>,
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> =
        ObserveChaptersUseCase(
            chapterRepository = chapterOps,
        )

    @Provides
    fun provideSyncComicMetadataUseCase(
        @AnilistEngine anilistMangaRepo: ComicGateway<ComicMetadataDto>,
        @MangadexEngine mangadexMangaRepo: ComicGateway<ComicMetadataDto>,
        @MangadexEngine mangadexChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>,
        @ComicInfoEngine comicInfoMangaRepo: ComicSyncGateway,
        @ComicInfoEngine comicInfoChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>,
    ): SyncComicMetadataUseCase =
        SyncComicMetadataUseCase(
            anilistMangaRepo,
            mangadexMangaRepo,
            mangadexChapterRepo,
            comicInfoMangaRepo,
            comicInfoChapterRepo,
        )
}
