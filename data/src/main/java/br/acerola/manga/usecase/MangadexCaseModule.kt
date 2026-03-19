package br.acerola.manga.usecase

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.di.MangadexFsOps
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
annotation class MangadexCase

@Module
@InstallIn(SingletonComponent::class)
object MangadexCaseModule {

    @Provides
    @MangadexCase
    fun provideSyncLibraryUseCase(
        @MangadexFsOps repository: MangaManagementRepository<MangaRemoteInfoDto>
    ): SyncLibraryUseCase<MangaRemoteInfoDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @MangadexCase
    fun provideObserveLibraryUseCase(
        @MangadexFsOps mangaOps: MangaManagementRepository<MangaRemoteInfoDto>
    ): ObserveLibraryUseCase<MangaRemoteInfoDto> {
        return ObserveLibraryUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @MangadexCase
    fun provideRescanMangaUseCase(
        @MangadexFsOps mangaOps: MangaManagementRepository<MangaRemoteInfoDto>
    ): RescanMangaUseCase<MangaRemoteInfoDto> {
        return RescanMangaUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @MangadexCase
    fun provideRescanMangaChaptersUseCase(
        @MangadexFsOps chapterOps: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto> {
        return RescanMangaChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    @MangadexCase
    fun provideGetChaptersUseCase(
        @MangadexFsOps chapterOps: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> {
        return ObserveChaptersUseCase(chapterRepository = chapterOps)
    }
}
