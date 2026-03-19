package br.acerola.manga.usecase

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.di.AnilistFsOps
import br.acerola.manga.repository.di.ComicInfoFsOps
import br.acerola.manga.repository.di.MangadexFsOps
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.repository.port.MangaManagementRepository
import br.acerola.manga.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.usecase.library.SyncLibraryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.usecase.library.RescanMangaChaptersUseCase
import br.acerola.manga.usecase.library.RescanMangaUseCase
import br.acerola.manga.usecase.metadata.SyncMangaMetadataUseCase
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
        @ComicInfoFsOps repository: MangaManagementRepository<MangaRemoteInfoDto>
    ): SyncLibraryUseCase<MangaRemoteInfoDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @ComicInfoCase
    fun provideObserveLibraryUseCase(
        @ComicInfoFsOps mangaOps: MangaManagementRepository<MangaRemoteInfoDto>
    ): ObserveLibraryUseCase<MangaRemoteInfoDto> {
        return ObserveLibraryUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @ComicInfoCase
    fun provideRescanMangaUseCase(
        @ComicInfoFsOps mangaOps: MangaManagementRepository<MangaRemoteInfoDto>
    ): RescanMangaUseCase<MangaRemoteInfoDto> {
        return RescanMangaUseCase(mangaRepository = mangaOps)
    }

    @Provides
    @ComicInfoCase
    fun provideRescanMangaChaptersUseCase(
        @ComicInfoFsOps chapterOps: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto> {
        return RescanMangaChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    @ComicInfoCase
    fun provideGetChaptersUseCase(
        @ComicInfoFsOps chapterOps: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> {
        return ObserveChaptersUseCase(chapterRepository = chapterOps)
    }

    @Provides
    fun provideSyncMangaMetadataUseCase(
        @AnilistFsOps anilistMangaRepo: MangaManagementRepository<MangaRemoteInfoDto>,
        @MangadexFsOps mangadexMangaRepo: MangaManagementRepository<MangaRemoteInfoDto>,
        @MangadexFsOps mangadexChapterRepo: ChapterManagementRepository<ChapterRemoteInfoPageDto>,
        @ComicInfoFsOps comicInfoMangaRepo: MangaManagementRepository<MangaRemoteInfoDto>,
        @ComicInfoFsOps comicInfoChapterRepo: ChapterManagementRepository<ChapterRemoteInfoPageDto>
    ): SyncMangaMetadataUseCase {
        return SyncMangaMetadataUseCase(
            anilistMangaRepo,
            mangadexMangaRepo,
            mangadexChapterRepo,
            comicInfoMangaRepo,
            comicInfoChapterRepo
        )
    }
}
