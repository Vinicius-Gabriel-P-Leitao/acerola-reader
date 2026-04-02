package br.acerola.manga.adapter.library

import br.acerola.manga.adapter.contract.gateway.MangaReadOnlyGateway
import br.acerola.manga.dto.view.MangaSummaryDto
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
    abstract fun bindSummaryEngine(engine: MangaSummaryEngine): MangaReadOnlyGateway<MangaSummaryDto>
}
