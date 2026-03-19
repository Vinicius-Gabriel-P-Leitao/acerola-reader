package br.acerola.manga.core.usecase

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.engine.di.AnilistFsOps
import br.acerola.manga.engine.di.ComicInfoFsOps
import br.acerola.manga.engine.di.MangadexFsOps
import br.acerola.manga.engine.port.ChapterPort
import br.acerola.manga.engine.port.MangaPort
import br.acerola.manga.core.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.core.usecase.library.RescanMangaChaptersUseCase
import br.acerola.manga.core.usecase.library.RescanMangaUseCase
import br.acerola.manga.core.usecase.library.SyncLibraryUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.core.usecase.metadata.SyncMangaMetadataUseCase
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
        @ComicInfoFsOps repository: MangaPort<MangaRemoteInfoDto>
    ): SyncLibraryUseCase<MangaRemoteInfoDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @ComicInfoCase
    fun provideObserveLibraryUseCase(
        @ComicInfoFsOps mangaOps: MangaPort<MangaRemoteInfoDto>
    ): ObserveLibraryUseCase<MangaRemoteInfoDto> {
        return ObserveLibraryUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @ComicInfoCase
    fun provideRescanMangaUseCase(
        @ComicInfoFsOps mangaOps: MangaPort<MangaRemoteInfoDto>
    ): RescanMangaUseCase<MangaRemoteInfoDto> {
        return RescanMangaUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @ComicInfoCase
    fun provideRescanMangaChaptersUseCase(
        @ComicInfoFsOps chapterOps: ChapterPort<ChapterRemoteInfoPageDto>
    ): RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto> {
        return RescanMangaChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    @ComicInfoCase
    fun provideGetChaptersUseCase(
        @ComicInfoFsOps chapterOps: ChapterPort<ChapterRemoteInfoPageDto>
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> {
        return ObserveChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    fun provideSyncMangaMetadataUseCase(
        @AnilistFsOps anilistMangaRepo: MangaPort<MangaRemoteInfoDto>,
        @MangadexFsOps mangadexMangaRepo: MangaPort<MangaRemoteInfoDto>,
        @MangadexFsOps mangadexChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>,
        @ComicInfoFsOps comicInfoMangaRepo: MangaPort<MangaRemoteInfoDto>,
        @ComicInfoFsOps comicInfoChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>
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
