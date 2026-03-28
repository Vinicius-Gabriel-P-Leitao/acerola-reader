package br.acerola.manga.core.usecase

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.adapter.metadata.mangadex.MangadexEngine
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
annotation class MangadexCase

@Module
@InstallIn(SingletonComponent::class)
object MangadexCaseModule {

    @Provides
    @MangadexCase
    fun provideSyncLibraryUseCase(
        @MangadexEngine repository: MangaGateway<MangaMetadataDto>
    ): SyncLibraryUseCase {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @MangadexCase
    fun provideObserveLibraryUseCase(
        @MangadexEngine repository: MangaGateway<MangaMetadataDto>
    ): ObserveLibraryUseCase<MangaMetadataDto> {
        return ObserveLibraryUseCase(
            mangaRepository = repository,
            syncGateway = repository
        )
    }

    @Provides
    @MangadexCase
    fun provideRescanMangaUseCase(
        @MangadexEngine mangaOps: MangaGateway<MangaMetadataDto>
    ): RescanMangaUseCase {
        return RescanMangaUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @MangadexCase
    fun provideRescanMangaChaptersUseCase(
        @MangadexEngine chapterOps: ChapterGateway<ChapterRemoteInfoPageDto>
    ): RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto> {
        return RescanMangaChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    @MangadexCase
    fun provideGetChaptersUseCase(
        @MangadexEngine chapterOps: ChapterGateway<ChapterRemoteInfoPageDto>
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> {
        return ObserveChaptersUseCase(
            chapterRepository = chapterOps
        )
    }
}
