package br.acerola.manga.engine.di

import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.engine.adapter.directory.ChapterArchiveAdapter
import br.acerola.manga.engine.adapter.directory.MangaDirectoryAdapter
import br.acerola.manga.engine.port.ChapterPort
import br.acerola.manga.engine.port.MangaPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class DirectoryFsOps

@Module
@InstallIn(SingletonComponent::class)
abstract class DirectoryModule {

    @Binds
    @Singleton
    @DirectoryFsOps
    abstract fun bindMangaDirectoryRepository(
        impl: MangaDirectoryAdapter
    ): MangaPort<MangaDirectoryDto>

    @Binds
    @Singleton
    @DirectoryFsOps
    abstract fun bindChapterArchiveRepository(
        int: ChapterArchiveAdapter
    ): ChapterPort<ChapterArchivePageDto>

}