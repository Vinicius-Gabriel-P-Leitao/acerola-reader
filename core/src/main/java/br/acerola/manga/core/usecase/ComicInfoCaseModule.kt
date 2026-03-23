package br.acerola.manga.core.usecase

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.adapter.metadata.anilist.AnilistEngine
import br.acerola.manga.adapter.metadata.comicinfo.ComicInfoEngine
import br.acerola.manga.adapter.metadata.mangadex.MangadexEngine
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
        @ComicInfoEngine repository: MangaGateway<MangaMetadataDto>
    ): SyncLibraryUseCase<MangaMetadataDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @ComicInfoCase
    fun provideObserveLibraryUseCase(
        @ComicInfoEngine mangaOps: MangaGateway<MangaMetadataDto>
    ): ObserveLibraryUseCase<MangaMetadataDto> {
        return ObserveLibraryUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @ComicInfoCase
    fun provideRescanMangaUseCase(
        @ComicInfoEngine mangaOps: MangaGateway<MangaMetadataDto>
    ): RescanMangaUseCase<MangaMetadataDto> {
        return RescanMangaUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @ComicInfoCase
    fun provideRescanMangaChaptersUseCase(
        @ComicInfoEngine chapterOps: ChapterGateway<ChapterRemoteInfoPageDto>
    ): RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto> {
        return RescanMangaChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    @ComicInfoCase
    fun provideGetChaptersUseCase(
        @ComicInfoEngine chapterOps: ChapterGateway<ChapterRemoteInfoPageDto>
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> {
        return ObserveChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    fun provideSyncMangaMetadataUseCase(
        @AnilistEngine anilistMangaRepo: MangaGateway<MangaMetadataDto>,
        @MangadexEngine mangadexMangaRepo: MangaGateway<MangaMetadataDto>,
        @MangadexEngine mangadexChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>,
        @ComicInfoEngine comicInfoMangaRepo: MangaGateway<MangaMetadataDto>,
        @ComicInfoEngine comicInfoChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>
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
