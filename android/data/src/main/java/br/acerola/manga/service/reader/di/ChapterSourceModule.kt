package br.acerola.manga.service.reader.di

import br.acerola.manga.service.reader.extract.CbrPageResolver
import br.acerola.manga.service.reader.extract.CbzPageResolver
import br.acerola.manga.service.reader.contract.PageSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

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
    abstract fun bindCbrService(
        impl: CbrPageResolver
    ): PageSource

    @Cbz
    @Binds
    abstract fun bindCbzService(
        impl: CbzPageResolver
    ): PageSource
}