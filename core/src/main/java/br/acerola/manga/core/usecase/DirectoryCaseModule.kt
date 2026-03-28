package br.acerola.manga.core.usecase

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.gateway.MangaGateway
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
        @DirectoryEngine repository: MangaGateway<MangaDirectoryDto>
    ): SyncLibraryUseCase {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @DirectoryCase
    fun provideObserveDirectoryUseCase(
        @DirectoryEngine repository: MangaGateway<MangaDirectoryDto>
    ): ObserveLibraryUseCase<MangaDirectoryDto> {
        return ObserveLibraryUseCase(
            mangaRepository = repository,
            syncGateway = repository
        )
    }

    @Provides
    @DirectoryCase
    fun provideRescanMangaUseCase(
        @DirectoryEngine mangaOps: MangaGateway<MangaDirectoryDto>
    ): RescanMangaUseCase {
        return RescanMangaUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @DirectoryCase
    fun provideRescanMangaChaptersUseCase(
        @DirectoryEngine chapterOps: ChapterGateway<ChapterArchivePageDto>
    ): RescanMangaChaptersUseCase<ChapterArchivePageDto> {
        return RescanMangaChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    @DirectoryCase
    fun provideGetChaptersUseCase(
        @DirectoryEngine chapterOps: ChapterGateway<ChapterArchivePageDto>
    ): ObserveChaptersUseCase<ChapterArchivePageDto> {
        return ObserveChaptersUseCase(
            chapterRepository = chapterOps
        )
    }
}
