package br.acerola.manga.adapter.di

import br.acerola.manga.adapter.impl.directory.ChapterArchiveEngine
import br.acerola.manga.adapter.impl.directory.MangaDirectoryEngine
import br.acerola.manga.adapter.port.ChapterPort
import br.acerola.manga.adapter.port.MangaPort
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
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
    abstract fun bindMangaDirectoryRepository(
        impl: MangaDirectoryEngine
    ): MangaPort<MangaDirectoryDto>

    @Binds
    @Singleton
    @DirectoryEngine
    abstract fun bindChapterArchiveRepository(
        int: ChapterArchiveEngine
    ): ChapterPort<ChapterArchivePageDto>

}