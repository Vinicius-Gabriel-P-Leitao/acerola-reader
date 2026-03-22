package br.acerola.manga.core.usecase.manga

import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.adapter.library.DirectoryEngine
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.message.IoError
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.service.metadata.CoverExtractionService
import javax.inject.Inject

class ExtractCoverFromChapterUseCase @Inject constructor(
    private val coverExtractionService: CoverExtractionService,
    @param:DirectoryEngine private val mangaPort: MangaPort<MangaDirectoryDto>,
) {
    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> {
        return coverExtractionService.extractFirstPageAsCover(mangaId)
            .mapLeft { ioError ->
                val cause = when (ioError) {
                    is IoError.FileReadError -> ioError.cause
                    is IoError.FileWriteError -> ioError.cause
                    is IoError.FileNotFound -> null
                }
                LibrarySyncError.UnexpectedError(cause ?: Exception(ioError.toString()))
            }
            .flatMap {
                mangaPort.refreshManga(mangaId)
            }
    }
}
