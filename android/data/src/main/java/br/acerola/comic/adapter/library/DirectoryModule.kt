package br.acerola.comic.adapter.library

import br.acerola.comic.adapter.contract.gateway.ChapterReadGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicLibraryWriteGateway
import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.adapter.contract.gateway.ComicRebuildGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.adapter.contract.gateway.VolumeGateway
import br.acerola.comic.adapter.library.engine.ChapterArchiveEngine
import br.acerola.comic.adapter.library.engine.ComicDirectoryEngine
import br.acerola.comic.adapter.library.engine.ComicLibraryStateEngine
import br.acerola.comic.adapter.library.engine.VolumeArchiveEngine
import br.acerola.comic.dto.archive.ChapterPageDto
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
    abstract fun bindMangaDirectorySingleSync(impl: ComicDirectoryEngine): ComicSingleSyncGateway

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindMangaDirectoryLibraryScan(impl: ComicDirectoryEngine): ComicLibraryScanGateway

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindMangaDirectoryRebuild(impl: ComicDirectoryEngine): ComicRebuildGateway

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindMangaDirectoryReadOnly(impl: ComicDirectoryEngine): ComicReadOnlyGateway<ComicDirectoryDto>

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindChapterArchiveSyncRepository(int: ChapterArchiveEngine): ChapterSyncGateway

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindChapterArchiveStatusRepository(int: ChapterArchiveEngine): ChapterSyncStatusGateway

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindChapterArchiveReadRepository(int: ChapterArchiveEngine): ChapterReadGateway<ChapterPageDto>

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindVolumeChapterRepository(impl: VolumeArchiveEngine): VolumeGateway

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindMangaLibraryWriteGateway(impl: ComicLibraryStateEngine): ComicLibraryWriteGateway
}
