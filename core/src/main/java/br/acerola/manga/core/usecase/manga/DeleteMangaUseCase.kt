package br.acerola.manga.core.usecase.manga

import arrow.core.Either
import br.acerola.manga.adapter.contract.gateway.MangaLibraryWriteGateway
import br.acerola.manga.adapter.library.DirectoryEngine
import br.acerola.manga.error.message.LibrarySyncError
import javax.inject.Inject

class DeleteMangaUseCase @Inject constructor(
    @param:DirectoryEngine private val gateway: MangaLibraryWriteGateway,
) {
    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> =
        gateway.deleteManga(mangaId)
}
