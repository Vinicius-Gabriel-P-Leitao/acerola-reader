package br.acerola.manga.usecase.di

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.di.DirectoryFsOps
import br.acerola.manga.repository.di.MangadexFsOps
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.usecase.chapter.GetChaptersUseCase
import br.acerola.manga.usecase.library.SyncLibraryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.usecase.manga.RescanMangaChaptersUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class DirectoryCase

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class MangadexCase

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @DirectoryCase
    fun provideDirectorySyncLibraryUseCase(
        @DirectoryFsOps repository: LibraryRepository<MangaDirectoryDto>
    ): SyncLibraryUseCase<MangaDirectoryDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @DirectoryCase
    fun provideDirectoryObserveLibraryUseCase(
        @DirectoryFsOps mangaOps: LibraryRepository.MangaOperations<MangaDirectoryDto>
    ): ObserveLibraryUseCase<MangaDirectoryDto> {
        return ObserveLibraryUseCase(mangaOperations = mangaOps)
    }

    @Provides
    @DirectoryCase
    fun provideDirectoryRescanMangaUseCase(
        @DirectoryFsOps mangaOps: LibraryRepository.MangaOperations<MangaDirectoryDto>
    ): RescanMangaChaptersUseCase<MangaDirectoryDto> {
        return RescanMangaChaptersUseCase(mangaOperations = mangaOps)
    }

    @Provides
    @DirectoryCase
    fun provideDirectoryGetChaptersUseCase(
        @DirectoryFsOps chapterOps: LibraryRepository.ChapterOperations<ChapterArchivePageDto>
    ): GetChaptersUseCase<ChapterArchivePageDto> {
        return GetChaptersUseCase(chapterOperations = chapterOps)
    }

    @Provides
    @MangadexCase
    fun provideMangadexSyncLibraryUseCase(
        @MangadexFsOps repository: LibraryRepository<MangaRemoteInfoDto>
    ): SyncLibraryUseCase<MangaRemoteInfoDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @MangadexCase
    fun provideMangadexObserveLibraryUseCase(
        @MangadexFsOps mangaOps: LibraryRepository.MangaOperations<MangaRemoteInfoDto>
    ): ObserveLibraryUseCase<MangaRemoteInfoDto> {
        return ObserveLibraryUseCase(mangaOperations = mangaOps)
    }

    @Provides
    @MangadexCase
    fun provideMangadexRescanMangaUseCase(
        @MangadexFsOps mangaOps: LibraryRepository.MangaOperations<MangaRemoteInfoDto>
    ): RescanMangaChaptersUseCase<MangaRemoteInfoDto> {
        return RescanMangaChaptersUseCase(mangaOperations = mangaOps)
    }

    @Provides
    @MangadexCase
    fun provideMangadexGetChaptersUseCase(
        @MangadexFsOps chapterOps: LibraryRepository.ChapterOperations<ChapterRemoteInfoPageDto>
    ): GetChaptersUseCase<ChapterRemoteInfoPageDto> {
        return GetChaptersUseCase(chapterOperations = chapterOps)
    }
}