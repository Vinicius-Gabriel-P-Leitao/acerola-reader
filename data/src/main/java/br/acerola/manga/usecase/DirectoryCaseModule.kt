package br.acerola.manga.usecase

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.repository.di.DirectoryFsOps
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.repository.port.MangaManagementRepository
import br.acerola.manga.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.usecase.library.SyncLibraryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.usecase.library.RescanMangaChaptersUseCase
import br.acerola.manga.usecase.library.RescanMangaUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class DirectoryCase

@Module
@InstallIn(SingletonComponent::class)
object DirectoryCaseModule {

    @Provides
    @DirectoryCase
    fun provideSyncLibraryUseCase(
        @DirectoryFsOps repository: MangaManagementRepository<MangaDirectoryDto>
    ): SyncLibraryUseCase<MangaDirectoryDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @DirectoryCase
    fun provideObserveLibraryUseCase(
        @DirectoryFsOps mangaOps: MangaManagementRepository<MangaDirectoryDto>
    ): ObserveLibraryUseCase<MangaDirectoryDto> {
        return ObserveLibraryUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @DirectoryCase
    fun provideRescanMangaUseCase(
        @DirectoryFsOps mangaOps: MangaManagementRepository<MangaDirectoryDto>
    ): RescanMangaUseCase<MangaDirectoryDto> {
        return RescanMangaUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @DirectoryCase
    fun provideRescanMangaChaptersUseCase(
        @DirectoryFsOps chapterOps: ChapterManagementRepository<ChapterArchivePageDto>
    ): RescanMangaChaptersUseCase<ChapterArchivePageDto> {
        return RescanMangaChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    @DirectoryCase
    fun provideGetChaptersUseCase(
        @DirectoryFsOps chapterOps: ChapterManagementRepository<ChapterArchivePageDto>
    ): ObserveChaptersUseCase<ChapterArchivePageDto> {
        return ObserveChaptersUseCase(chapterRepository = chapterOps)
    }
}
