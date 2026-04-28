package br.acerola.comic.usecase.template

import arrow.core.Either
import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.service.template.ChapterNameProcessor
import javax.inject.Inject

class UpdateTemplateUseCase
    @Inject
    constructor(
        private val service: ChapterNameProcessor,
    ) {
        suspend operator fun invoke(
            id: Long,
            label: String,
            pattern: String,
        ): Either<TemplateError, Unit> = service.updateTemplate(id, label, pattern)
    }
