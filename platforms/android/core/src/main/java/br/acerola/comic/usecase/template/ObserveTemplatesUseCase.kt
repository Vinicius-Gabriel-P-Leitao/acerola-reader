package br.acerola.comic.usecase.template

import br.acerola.comic.dto.archive.ArchiveTemplateDto
import br.acerola.comic.service.template.ChapterNameProcessor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTemplatesUseCase
    @Inject
    constructor(
        private val service: ChapterNameProcessor,
    ) {
        operator fun invoke(): Flow<List<ArchiveTemplateDto>> = service.observeTemplatesAsDto()
    }
