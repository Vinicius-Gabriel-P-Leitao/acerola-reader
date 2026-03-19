package br.acerola.manga.core.usecase

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.adapter.di.AnilistEngine
import br.acerola.manga.adapter.di.ComicInfoEngine
import br.acerola.manga.adapter.di.MangadexEngine
import br.acerola.manga.adapter.port.ChapterPort
import br.acerola.manga.adapter.port.MangaPort
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
        @ComicInfoEngine repository: MangaPort<MangaRemoteInfoDto>
    ): SyncLibraryUseCase<MangaRemoteInfoDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @ComicInfoCase
    fun provideObserveLibraryUseCase(
        @ComicInfoEngine mangaOps: MangaPort<MangaRemoteInfoDto>
    ): ObserveLibraryUseCase<MangaRemoteInfoDto> {
        return ObserveLibraryUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @ComicInfoCase
    fun provideRescanMangaUseCase(
        @ComicInfoEngine mangaOps: MangaPort<MangaRemoteInfoDto>
    ): RescanMangaUseCase<MangaRemoteInfoDto> {
        return RescanMangaUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @ComicInfoCase
    fun provideRescanMangaChaptersUseCase(
        @ComicInfoEngine chapterOps: ChapterPort<ChapterRemoteInfoPageDto>
    ): RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto> {
        return RescanMangaChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    @ComicInfoCase
    fun provideGetChaptersUseCase(
        @ComicInfoEngine chapterOps: ChapterPort<ChapterRemoteInfoPageDto>
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> {
        return ObserveChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    fun provideSyncMangaMetadataUseCase(
        @AnilistEngine anilistMangaRepo: MangaPort<MangaRemoteInfoDto>,
        @MangadexEngine mangadexMangaRepo: MangaPort<MangaRemoteInfoDto>,
        @MangadexEngine mangadexChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>,
        @ComicInfoEngine comicInfoMangaRepo: MangaPort<MangaRemoteInfoDto>,
        @ComicInfoEngine comicInfoChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>
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
