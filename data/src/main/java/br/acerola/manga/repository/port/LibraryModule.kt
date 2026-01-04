package br.acerola.manga.repository.port

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.adapter.local.chapter.ChapterArchiveOperation
import br.acerola.manga.repository.adapter.local.chapter.MangadexChapterRemoteInfoOperation
import br.acerola.manga.repository.adapter.local.manga.MangaDirectoryOperation
import br.acerola.manga.repository.adapter.local.manga.MangadexRemoteInfoOperation
import br.acerola.manga.repository.adapter.local.sync.ArchiveSyncService
import br.acerola.manga.repository.adapter.local.sync.MangadexSyncService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class DirectoryFsOps

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class MangadexFsOps

/**
 * Esse modulo é feito para abstrair chamadas de API e uso de DAO, servem para ser usados no ViewModel, ao contrario do
 *
 * [ApiModule]
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {
    @Binds
    @Singleton
    @DirectoryFsOps
    abstract fun bindArchiveSyncService(
        impl: ArchiveSyncService
    ): LibraryRepository<MangaDirectoryDto>

    @Binds
    @Singleton
    @DirectoryFsOps
    abstract fun bindMangaDirectoryOperations(
        impl: MangaDirectoryOperation
    ): LibraryRepository.MangaOperations<MangaDirectoryDto>

    @Binds
    @Singleton
    @DirectoryFsOps
    abstract fun bindChapterArchiveOperation(
        impl: ChapterArchiveOperation
    ): LibraryRepository.ChapterOperations<ChapterArchivePageDto>

    @Binds
    @Singleton
    @MangadexFsOps
    abstract fun bindMangaDexSyncService(
        impl: MangadexSyncService
    ): LibraryRepository<MangaRemoteInfoDto>

    @Binds
    @Singleton
    @MangadexFsOps
    abstract fun bindMangadexRemoteInfoOperation(
        impl: MangadexRemoteInfoOperation
    ): LibraryRepository.MangaOperations<MangaRemoteInfoDto>

    @Binds
    @Singleton
    @MangadexFsOps
    abstract fun bindMangadexChapterRemoteInfoOperation(
        impl: MangadexChapterRemoteInfoOperation
    ): LibraryRepository.ChapterOperations<ChapterRemoteInfoPageDto>
}