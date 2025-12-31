package br.acerola.manga.domain.service.library

import br.acerola.manga.domain.service.library.chapter.FileChapterOperation
import br.acerola.manga.domain.service.library.manga.FolderMangaOperation
import br.acerola.manga.domain.service.library.manga.MangaMetadataOperation
import br.acerola.manga.domain.service.library.sync.ArchiveSyncService
import br.acerola.manga.domain.service.library.sync.MangadexSyncService
import br.acerola.manga.domain.dto.archive.ChapterPageDto
import br.acerola.manga.domain.dto.archive.MangaFolderDto
import br.acerola.manga.domain.dto.metadata.manga.MangaMetadataDto
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    // NOTE: Mangadex
    @Binds
    @Singleton
    abstract fun bindMangaDexSyncService(
        impl: MangadexSyncService
    ): LibraryRepository<MangaMetadataDto>

    @Binds
    @Singleton
    abstract fun bindMangaMetadataOperation(
        impl: MangaMetadataOperation
    ): LibraryRepository.MangaOperations<MangaMetadataDto>

    // NOTE: Arquivos
    @Binds
    @Singleton
    abstract fun bindArchiveSyncService(
        impl: ArchiveSyncService
    ): LibraryRepository<MangaFolderDto>

    @Binds
    @Singleton
    abstract fun bindFolderMangaOperations(
        impl: FolderMangaOperation
    ): LibraryRepository.MangaOperations<MangaFolderDto>

    @Binds
    @Singleton
    abstract fun bindFileChapterOperation(
        impl: FileChapterOperation
    ): LibraryRepository.ChapterOperations<ChapterPageDto>
}