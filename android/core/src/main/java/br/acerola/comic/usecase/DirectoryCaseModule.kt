package br.acerola.comic.usecase

import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.library.DirectoryEngine
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
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
@Retention(AnnotationRetention.BINARY)
public annotation class DirectoryCase

@Module
@InstallIn(SingletonComponent::class)
object DirectoryCaseModule {
    @Provides
    @DirectoryCase
    fun provideSyncLibraryUseCase(
        @DirectoryEngine repository: ComicGateway<ComicDirectoryDto>,
    ): SyncLibraryUseCase = SyncLibraryUseCase(repository)

    @Provides
    @DirectoryCase
    fun provideObserveDirectoryUseCase(
        @DirectoryEngine repository: ComicGateway<ComicDirectoryDto>,
    ): ObserveLibraryUseCase<ComicDirectoryDto> =
        ObserveLibraryUseCase(
            mangaRepository = repository,
            syncGateway = repository,
        )

    @Provides
    @DirectoryCase
    fun provideRescanComicUseCase(
        @DirectoryEngine mangaOps: ComicGateway<ComicDirectoryDto>,
    ): RescanComicUseCase =
        RescanComicUseCase(
            mangaRepository = mangaOps,
        )

    @Provides
    @DirectoryCase
    fun provideRescanComicChaptersUseCase(
        @DirectoryEngine chapterOps: ChapterGateway<ChapterArchivePageDto>,
    ): RescanComicChaptersUseCase<ChapterArchivePageDto> =
        RescanComicChaptersUseCase(
            chapterRepository = chapterOps,
        )

    @Provides
    @DirectoryCase
    fun provideGetChaptersUseCase(
        @DirectoryEngine chapterOps: ChapterGateway<ChapterArchivePageDto>,
    ): ObserveChaptersUseCase<ChapterArchivePageDto> =
        ObserveChaptersUseCase(
            chapterRepository = chapterOps,
        )
}
