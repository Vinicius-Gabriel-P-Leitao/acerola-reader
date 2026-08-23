package br.acerola.comic.usecase.metadata

import br.acerola.comic.service.metadata.MetadataCleaner
import javax.inject.Inject

class ClearMetadataUseCase
    @Inject
    constructor(
        private val metadataCleaner: MetadataCleaner,
    ) {
        suspend operator fun invoke(directoryId: Long) {
            metadataCleaner.clearMetadata(directoryId)
        }
    }
