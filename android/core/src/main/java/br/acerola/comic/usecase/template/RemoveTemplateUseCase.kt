package br.acerola.comic.usecase.template

import arrow.core.Either
import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.service.template.ChapterNameProcessor
import javax.inject.Inject

class RemoveTemplateUseCase @Inject constructor(
    private val service: ChapterNameProcessor
) {
    suspend operator fun invoke(id: Long): Either<TemplateError, Unit> = 
        service.removeTemplate(id)
}
