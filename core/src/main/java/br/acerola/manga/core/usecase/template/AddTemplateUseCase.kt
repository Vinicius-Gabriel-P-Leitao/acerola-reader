package br.acerola.manga.core.usecase.template

import arrow.core.Either
import br.acerola.manga.error.message.TemplateError
import br.acerola.manga.service.template.ChapterNameProcessor
import javax.inject.Inject

class AddTemplateUseCase @Inject constructor(
    private val service: ChapterNameProcessor
) {
    suspend operator fun invoke(label: String, pattern: String): Either<TemplateError, Unit> = 
        service.addTemplate(label, pattern)
}
