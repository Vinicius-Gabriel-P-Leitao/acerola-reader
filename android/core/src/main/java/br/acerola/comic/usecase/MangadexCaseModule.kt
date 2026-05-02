package br.acerola.comic.usecase

import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
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
        @MangadexEngine repository: ComicGateway<ComicMetadataDto>,
    ): SyncLibraryUseCase = SyncLibraryUseCase(repository)

    @Provides
    @MangadexCase
    fun provideObserveLibraryUseCase(
        @MangadexEngine repository: ComicGateway<ComicMetadataDto>,
    ): ObserveLibraryUseCase<ComicMetadataDto> =
        ObserveLibraryUseCase(
            comicRepository = repository,
            syncGateway = repository,
        )

    @Provides
    @MangadexCase
    fun provideRescanComicUseCase(
        @MangadexEngine comicOps: ComicGateway<ComicMetadataDto>,
    ): RescanComicUseCase =
        RescanComicUseCase(
            comicRepository = comicOps,
        )

    @Provides
    @MangadexCase
    fun provideRescanComicChaptersUseCase(
        @MangadexEngine chapterOps: ChapterGateway<ChapterRemoteInfoPageDto>,
    ): RescanComicChaptersUseCase<ChapterRemoteInfoPageDto> =
        RescanComicChaptersUseCase(
            chapterRepository = chapterOps,
        )

    @Provides
    @MangadexCase
    fun provideGetChaptersUseCase(
        @MangadexEngine chapterOps: ChapterGateway<ChapterRemoteInfoPageDto>,
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> =
        ObserveChaptersUseCase(
            chapterRepository = chapterOps,
        )
}
