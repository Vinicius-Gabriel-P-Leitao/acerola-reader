package br.acerola.manga.core.usecase.template

import arrow.core.Either
import br.acerola.manga.error.message.TemplateError
import br.acerola.manga.service.template.ChapterTemplateService
import javax.inject.Inject

class RemoveTemplateUseCase @Inject constructor(
    private val service: ChapterTemplateService
) {
    suspend operator fun invoke(id: Long): Either<TemplateError, Unit> = 
        service.removeTemplate(id)
}
