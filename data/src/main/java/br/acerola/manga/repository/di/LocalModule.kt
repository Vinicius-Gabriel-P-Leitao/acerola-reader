package br.acerola.manga.repository.di

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.adapter.local.chapter.ChapterArchiveRepository
import br.acerola.manga.repository.adapter.local.chapter.MangadexChapterRepository
import br.acerola.manga.repository.adapter.local.manga.MangaDirectoryRepository
import br.acerola.manga.repository.adapter.local.manga.MangadexMangaRepository
import br.acerola.manga.repository.adapter.local.sync.DirectorySyncSyncRepository
import br.acerola.manga.repository.adapter.local.sync.MangadexSyncSyncRepository
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.repository.port.LibrarySyncRepository
import br.acerola.manga.repository.port.MangaManagementRepository
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
    abstract fun bindDirectorySyncRepository(
        impl: DirectorySyncSyncRepository
    ): LibrarySyncRepository<MangaDirectoryDto>

    @Binds
    @Singleton
    @DirectoryFsOps
    abstract fun bindMangaDirectoryRepository(
        impl: MangaDirectoryRepository
    ): MangaManagementRepository<MangaDirectoryDto>

    @Binds
    @Singleton
    @DirectoryFsOps
    abstract fun bindChapterArchiveRepository(
        int: ChapterArchiveRepository
    ): ChapterManagementRepository<ChapterArchivePageDto>


    @Binds
    @Singleton
    @MangadexFsOps
    abstract fun bindMangadexSyncRepository(
        impl: MangadexSyncSyncRepository
    ): LibrarySyncRepository<MangaRemoteInfoDto>

    @Binds
    @Singleton
    @MangadexFsOps
    abstract fun bindMangadexMangaRepository(
        impl: MangadexMangaRepository
    ): MangaManagementRepository<MangaRemoteInfoDto>

    @Binds
    @Singleton
    @MangadexFsOps
    abstract fun bindMangadexChapterRepository(
        impl: MangadexChapterRepository
    ): ChapterManagementRepository<ChapterRemoteInfoPageDto>
}