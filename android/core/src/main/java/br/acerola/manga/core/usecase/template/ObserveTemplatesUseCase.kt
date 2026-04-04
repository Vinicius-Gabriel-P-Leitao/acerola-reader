package br.acerola.manga.core.usecase.template

import br.acerola.manga.dto.archive.ChapterTemplateDto
import br.acerola.manga.service.template.ChapterNameProcessor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTemplatesUseCase @Inject constructor(
    private val service: ChapterNameProcessor
) {
    operator fun invoke(): Flow<List<ChapterTemplateDto>> = service.observeTemplatesAsDto()
}
