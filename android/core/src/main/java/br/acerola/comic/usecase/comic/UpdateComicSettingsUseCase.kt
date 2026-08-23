package br.acerola.comic.usecase.comic

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicLibraryWriteGateway
import br.acerola.comic.adapter.library.DirectoryEngine
import br.acerola.comic.error.message.LibrarySyncError
import javax.inject.Inject

class UpdateComicSettingsUseCase
    @Inject
    constructor(
        @param:DirectoryEngine private val gateway: ComicLibraryWriteGateway,
    ) {
        suspend operator fun invoke(
            comicId: Long,
            externalSyncEnabled: Boolean,
        ): Either<LibrarySyncError, Unit> = gateway.updateMangaSettings(comicId, externalSyncEnabled)
    }
