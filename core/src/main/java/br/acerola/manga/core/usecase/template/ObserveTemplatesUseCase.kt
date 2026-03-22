package br.acerola.manga.core.usecase.template

import br.acerola.manga.local.entity.archive.ChapterTemplateEntity
import br.acerola.manga.service.template.ChapterTemplateService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTemplatesUseCase @Inject constructor(
    private val service: ChapterTemplateService
) {
    operator fun invoke(): Flow<List<ChapterTemplateEntity>> = service.observeTemplates()
}
