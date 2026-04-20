package br.acerola.comic.usecase.comic

import arrow.core.Either
import arrow.core.flatMap
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.library.DirectoryEngine
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.error.message.IoError
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.service.metadata.CoverExtractor
import javax.inject.Inject

class CoverFromChapterUseCase
    @Inject
    constructor(
        private val coverExtractor: CoverExtractor,
        @param:DirectoryEngine private val mangaGateway: ComicGateway<ComicDirectoryDto>,
    ) {
        suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> =
            coverExtractor
                .extractFirstPageAsCover(mangaId)
                .mapLeft { ioError ->
                    val cause =
                        when (ioError) {
                            is IoError.FileReadError -> ioError.cause
                            is IoError.FileWriteError -> ioError.cause
                            is IoError.FileNotFound -> null
                        }
                    LibrarySyncError.UnexpectedError(cause ?: Exception(ioError.toString()))
                }.flatMap {
                    mangaGateway.refreshManga(mangaId)
                }
    }
