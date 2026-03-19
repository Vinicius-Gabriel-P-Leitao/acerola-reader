package br.acerola.manga.core.usecase

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.adapter.di.MangadexEngine
import br.acerola.manga.adapter.port.ChapterPort
import br.acerola.manga.adapter.port.MangaPort
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
        @MangadexEngine repository: MangaPort<MangaRemoteInfoDto>
    ): SyncLibraryUseCase<MangaRemoteInfoDto> {
        return SyncLibraryUseCase(repository)
    }

    @Provides
    @MangadexCase
    fun provideObserveLibraryUseCase(
        @MangadexEngine mangaOps: MangaPort<MangaRemoteInfoDto>
    ): ObserveLibraryUseCase<MangaRemoteInfoDto> {
        return ObserveLibraryUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @MangadexCase
    fun provideRescanMangaUseCase(
        @MangadexEngine mangaOps: MangaPort<MangaRemoteInfoDto>
    ): RescanMangaUseCase<MangaRemoteInfoDto> {
        return RescanMangaUseCase(
            mangaRepository = mangaOps
        )
    }

    @Provides
    @MangadexCase
    fun provideRescanMangaChaptersUseCase(
        @MangadexEngine chapterOps: ChapterPort<ChapterRemoteInfoPageDto>
    ): RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto> {
        return RescanMangaChaptersUseCase(
            chapterRepository = chapterOps
        )
    }

    @Provides
    @MangadexCase
    fun provideGetChaptersUseCase(
        @MangadexEngine chapterOps: ChapterPort<ChapterRemoteInfoPageDto>
    ): ObserveChaptersUseCase<ChapterRemoteInfoPageDto> {
        return ObserveChaptersUseCase(
            chapterRepository = chapterOps
        )
    }
}
