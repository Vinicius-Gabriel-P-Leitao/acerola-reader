package br.acerola.comic.usecase

import br.acerola.comic.adapter.contract.gateway.ChapterReadGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.adapter.contract.gateway.ComicRebuildGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.adapter.contract.gateway.VolumeGateway
import br.acerola.comic.adapter.library.DirectoryEngine
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.chapter.ObserveVolumeChaptersUseCase
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
@Retention(AnnotationRetention.BINARY)
public annotation class DirectoryCase

@Module
@InstallIn(SingletonComponent::class)
object DirectoryCaseModule {
    @Provides
    @DirectoryCase
    fun provideSyncLibraryUseCase(
        @DirectoryEngine scanGateway: ComicLibraryScanGateway,
        @DirectoryEngine rebuildGateway: ComicRebuildGateway,
    ): SyncLibraryUseCase = SyncLibraryUseCase(scanGateway = scanGateway, rebuildGateway = rebuildGateway)

    @Provides
    @DirectoryCase
    fun provideObserveDirectoryUseCase(
        @DirectoryEngine repository: ComicReadOnlyGateway<ComicDirectoryDto>,
        @DirectoryEngine syncStatus: ComicSingleSyncGateway,
    ): ObserveLibraryUseCase<ComicDirectoryDto> =
        ObserveLibraryUseCase(
            comicRepository = repository,
            syncGateway = syncStatus,
        )

    @Provides
    @DirectoryCase
    fun provideRescanComicUseCase(
        @DirectoryEngine comicOps: ComicSingleSyncGateway,
    ): RescanComicUseCase =
        RescanComicUseCase(
            comicRepository = comicOps,
        )

    @Provides
    @DirectoryCase
    fun provideRescanComicChaptersUseCase(
        @DirectoryEngine chapterOps: ChapterSyncGateway,
    ): RescanComicChaptersUseCase =
        RescanComicChaptersUseCase(
            chapterRepository = chapterOps,
        )

    @Provides
    @DirectoryCase
    fun provideGetChaptersUseCase(
        @DirectoryEngine readOps: ChapterReadGateway<ChapterPageDto>,
        @DirectoryEngine statusOps: ChapterSyncStatusGateway,
    ): ObserveChaptersUseCase<ChapterPageDto> =
        ObserveChaptersUseCase(
            readGateway = readOps,
            syncStatusGateway = statusOps,
        )

    @Provides
    @DirectoryCase
    fun provideObserveVolumeChaptersUseCase(
        @DirectoryEngine volumeOps: VolumeGateway,
    ): ObserveVolumeChaptersUseCase =
        ObserveVolumeChaptersUseCase(
            volumeGateway = volumeOps,
        )
}
