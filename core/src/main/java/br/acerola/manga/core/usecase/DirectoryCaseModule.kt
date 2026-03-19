package br.acerola.manga.core.usecase

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.adapter.contract.ChapterPort
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.adapter.library.DirectoryEngine
import br.acerola.manga.core.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.core.usecase.library.RescanMangaChaptersUseCase
import br.acerola.manga.core.usecase.library.RescanMangaUseCase
import br.acerola.manga.core.usecase.library.SyncLibraryUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
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
        @DirectoryEngine repository: MangaPort<MangaDirectoryDto>
    ): SyncLibraryUseCase<MangaDirectoryDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @DirectoryCase
    fun provideObserveLibraryUseCase(
        @DirectoryEngine mangaOps: MangaPort<MangaDirectoryDto>
    ): ObserveLibraryUseCase<MangaDirectoryDto> {
        return ObserveLibraryUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @DirectoryCase
    fun provideRescanMangaUseCase(
        @DirectoryEngine mangaOps: MangaPort<MangaDirectoryDto>
    ): RescanMangaUseCase<MangaDirectoryDto> {
        return RescanMangaUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @DirectoryCase
    fun provideRescanMangaChaptersUseCase(
        @DirectoryEngine chapterOps: ChapterPort<ChapterArchivePageDto>
    ): RescanMangaChaptersUseCase<ChapterArchivePageDto> {
        return RescanMangaChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    @DirectoryCase
    fun provideGetChaptersUseCase(
        @DirectoryEngine chapterOps: ChapterPort<ChapterArchivePageDto>
    ): ObserveChaptersUseCase<ChapterArchivePageDto> {
        return ObserveChaptersUseCase(
            chapterRepository = chapterOps
        )
    }
}
