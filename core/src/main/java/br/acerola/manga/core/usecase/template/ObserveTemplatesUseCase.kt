package br.acerola.manga.core.usecase.template

import br.acerola.manga.local.entity.archive.ChapterTemplate
import br.acerola.manga.service.template.ChapterNameProcessor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTemplatesUseCase @Inject constructor(
    private val service: ChapterNameProcessor
) {
    operator fun invoke(): Flow<List<ChapterTemplate>> = service.observeTemplates()
}
