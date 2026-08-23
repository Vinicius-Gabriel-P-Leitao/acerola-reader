package br.acerola.comic.adapter.library

import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.adapter.library.engine.ComicSummaryEngine
import br.acerola.comic.dto.view.ComicSummaryDto
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SummaryEngine

@Module
@InstallIn(SingletonComponent::class)
abstract class SummaryModule {
    @Binds
    @Singleton
    @SummaryEngine
    abstract fun bindSummaryEngine(engine: ComicSummaryEngine): ComicReadOnlyGateway<ComicSummaryDto>
}
