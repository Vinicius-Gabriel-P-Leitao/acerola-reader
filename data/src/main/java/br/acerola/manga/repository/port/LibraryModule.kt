package br.acerola.manga.repository.port

import br.acerola.manga.dto.archive.ChapterPageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.adapter.local.chapter.ChapterArchiveOperation
import br.acerola.manga.repository.adapter.local.manga.MangaDirectoryOperation
import br.acerola.manga.repository.adapter.local.manga.ChapterRemoteInfoOperation
import br.acerola.manga.repository.adapter.local.sync.ArchiveSyncService
import br.acerola.manga.repository.adapter.local.sync.MangadexSyncService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {

    // NOTE: Mangadex
    @Binds
    @Singleton
    abstract fun bindMangaDexSyncService(
        impl: MangadexSyncService
    ): LibraryRepository<MangaRemoteInfoDto>

    @Binds
    @Singleton
    abstract fun bindMangaRemoteInfoOperation(
        impl: ChapterRemoteInfoOperation
    ): LibraryRepository.MangaOperations<MangaRemoteInfoDto>

    // NOTE: Arquivos
    @Binds
    @Singleton
    abstract fun bindArchiveSyncService(
        impl: ArchiveSyncService
    ): LibraryRepository<MangaDirectoryDto>

    @Binds
    @Singleton
    abstract fun bindMangaDirectoryOperations(
        impl: MangaDirectoryOperation
    ): LibraryRepository.MangaOperations<MangaDirectoryDto>

    @Binds
    @Singleton
    abstract fun bindFileChapterOperation(
        impl: ChapterArchiveOperation
    ): LibraryRepository.ChapterOperations<ChapterPageDto>
}