package br.acerola.manga.usecase.di

import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.di.ComicInfoFsOps
import br.acerola.manga.repository.di.DirectoryFsOps
import br.acerola.manga.repository.di.MangadexFsOps
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.repository.port.MangaManagementRepository
import br.acerola.manga.usecase.chapter.GetChaptersUseCase
import br.acerola.manga.usecase.library.SyncLibraryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.usecase.manga.RescanMangaChaptersUseCase
import br.acerola.manga.usecase.manga.RescanMangaUseCase
import br.acerola.manga.usecase.metadata.SyncMangaMetadataUseCase
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

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class ComicInfoCase

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @DirectoryCase
    fun provideDirectorySyncLibraryUseCase(
        @DirectoryFsOps repository: MangaManagementRepository<MangaDirectoryDto>
    ): SyncLibraryUseCase<MangaDirectoryDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @DirectoryCase
    fun provideDirectoryObserveLibraryUseCase(
        @DirectoryFsOps mangaOps: MangaManagementRepository<MangaDirectoryDto>
    ): ObserveLibraryUseCase<MangaDirectoryDto> {
        return ObserveLibraryUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @DirectoryCase
    fun provideDirectoryRescanMangaUseCase(
        @DirectoryFsOps mangaOps: MangaManagementRepository<MangaDirectoryDto>
    ): RescanMangaUseCase<MangaDirectoryDto> {
        return RescanMangaUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @DirectoryCase
    fun provideDirectoryRescanMangaChaptersUseCase(
        @DirectoryFsOps chapterOps: ChapterManagementRepository<ChapterArchivePageDto>
    ): RescanMangaChaptersUseCase<ChapterArchivePageDto> {
        return RescanMangaChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    @DirectoryCase
    fun provideDirectoryGetChaptersUseCase(
        @DirectoryFsOps chapterOps: ChapterManagementRepository<ChapterArchivePageDto>
    ): GetChaptersUseCase<ChapterArchivePageDto> {
        return GetChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    @MangadexCase
    fun provideMangadexSyncLibraryUseCase(
        @MangadexFsOps repository: MangaManagementRepository<MangaRemoteInfoDto>
    ): SyncLibraryUseCase<MangaRemoteInfoDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @MangadexCase
    fun provideMangadexObserveLibraryUseCase(
        @MangadexFsOps mangaOps: MangaManagementRepository<MangaRemoteInfoDto>
    ): ObserveLibraryUseCase<MangaRemoteInfoDto> {
        return ObserveLibraryUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @MangadexCase
    fun provideMangadexRescanMangaUseCase(
        @MangadexFsOps mangaOps: MangaManagementRepository<MangaRemoteInfoDto>
    ): RescanMangaUseCase<MangaRemoteInfoDto> {
        return RescanMangaUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @MangadexCase
    fun provideMangadexRescanMangaChaptersUseCase(
        @MangadexFsOps chapterOps: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto> {
        return RescanMangaChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    @MangadexCase
    fun provideMangadexGetChaptersUseCase(
        @MangadexFsOps chapterOps: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): GetChaptersUseCase<ChapterRemoteInfoPageDto> {
        return GetChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    @ComicInfoCase
    fun provideComicInfoSyncLibraryUseCase(
        @ComicInfoFsOps repository: MangaManagementRepository<MangaRemoteInfoDto>
    ): SyncLibraryUseCase<MangaRemoteInfoDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @ComicInfoCase
    fun provideComicInfoObserveLibraryUseCase(
        @ComicInfoFsOps mangaOps: MangaManagementRepository<MangaRemoteInfoDto>
    ): ObserveLibraryUseCase<MangaRemoteInfoDto> {
        return ObserveLibraryUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @ComicInfoCase
    fun provideComicInfoRescanMangaUseCase(
        @ComicInfoFsOps mangaOps: MangaManagementRepository<MangaRemoteInfoDto>
    ): RescanMangaUseCase<MangaRemoteInfoDto> {
        return RescanMangaUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @ComicInfoCase
    fun provideComicInfoRescanMangaChaptersUseCase(
        @ComicInfoFsOps chapterOps: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto> {
        return RescanMangaChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    @ComicInfoCase
    fun provideComicInfoGetChaptersUseCase(
        @ComicInfoFsOps chapterOps: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): GetChaptersUseCase<ChapterRemoteInfoPageDto> {
        return GetChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    fun provideSyncMangaMetadataUseCase(
        @MangadexFsOps mangadexMangaRepo: MangaManagementRepository<MangaRemoteInfoDto>,
        @MangadexFsOps mangadexChapterRepo: ChapterManagementRepository<ChapterRemoteInfoPageDto>,
        @ComicInfoFsOps comicInfoMangaRepo: MangaManagementRepository<MangaRemoteInfoDto>,
        @ComicInfoFsOps comicInfoChapterRepo: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): SyncMangaMetadataUseCase {
        return SyncMangaMetadataUseCase(
            mangadexMangaRepo,
            mangadexChapterRepo,
            comicInfoMangaRepo,
            comicInfoChapterRepo
        )
    }
}
