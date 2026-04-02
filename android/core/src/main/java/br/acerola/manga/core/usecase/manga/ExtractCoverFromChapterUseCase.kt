package br.acerola.manga.core.usecase.manga

import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.adapter.library.DirectoryEngine
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.message.IoError
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.service.metadata.CoverExtractor
import javax.inject.Inject

class ExtractCoverFromChapterUseCase @Inject constructor(
    private val coverExtractor: CoverExtractor,
    @param:DirectoryEngine private val mangaGateway: MangaGateway<MangaDirectoryDto>,
) {
    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> {
        return coverExtractor.extractFirstPageAsCover(mangaId)
            .mapLeft { ioError ->
                val cause = when (ioError) {
                    is IoError.FileReadError -> ioError.cause
                    is IoError.FileWriteError -> ioError.cause
                    is IoError.FileNotFound -> null
                }
                LibrarySyncError.UnexpectedError(cause ?: Exception(ioError.toString()))
            }
            .flatMap {
                mangaGateway.refreshManga(mangaId)
            }
    }
}
