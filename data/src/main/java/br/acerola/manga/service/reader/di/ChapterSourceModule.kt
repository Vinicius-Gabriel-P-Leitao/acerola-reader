package br.acerola.manga.service.reader.di

import br.acerola.manga.service.reader.extract.CbrChapterSourceService
import br.acerola.manga.service.reader.extract.CbzChapterSourceService
import br.acerola.manga.service.reader.port.ChapterSourceService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class Cbr

@Qualifier
@Retention(value = AnnotationRetention.BINARY)
annotation class Cbz

@Module
@InstallIn(SingletonComponent::class)
abstract class ChapterSourceModule {

    @Cbr
    @Binds
    @Singleton
    abstract fun bindCbrService(
        impl: CbrChapterSourceService
    ): ChapterSourceService

    @Cbz
    @Binds
    @Singleton
    abstract fun bindCbzService(
        impl: CbzChapterSourceService
    ): ChapterSourceService

}