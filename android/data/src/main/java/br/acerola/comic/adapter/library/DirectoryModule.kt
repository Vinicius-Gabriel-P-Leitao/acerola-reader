package br.acerola.comic.adapter.library

import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.contract.gateway.ComicLibraryWriteGateway
import br.acerola.comic.adapter.contract.gateway.VolumeChapterGateway
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class DirectoryEngine

@Module
@InstallIn(SingletonComponent::class)
abstract class DirectoryModule {
    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindMangaDirectoryRepository(impl: ComicDirectoryEngine): ComicGateway<ComicDirectoryDto>

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindChapterArchiveRepository(int: ChapterArchiveEngine): ChapterGateway<ChapterArchivePageDto>

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindVolumeChapterRepository(impl: ChapterArchiveEngine): VolumeChapterGateway

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindMangaLibraryWriteGateway(impl: ComicLibraryStateEngine): ComicLibraryWriteGateway
}
