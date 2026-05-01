package br.acerola.comic.usecase.metadata

import arrow.core.Either
import br.acerola.comic.error.message.IoError
import br.acerola.comic.service.metadata.CoverExtractor
import javax.inject.Inject

class ExtractVolumeCoverUseCase
    @Inject
    constructor(
        private val coverExtractor: CoverExtractor,
    ) {
    suspend operator fun invoke(comicId: Long, volumeId: Long): Either<IoError, Unit> {
        return coverExtractor.extractVolumeCover(comicId, volumeId)
    }
}
