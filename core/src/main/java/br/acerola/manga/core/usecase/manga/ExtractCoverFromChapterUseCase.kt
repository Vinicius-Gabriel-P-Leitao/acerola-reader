package br.acerola.manga.core.usecase.manga

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.adapter.contract.ChapterPort
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.adapter.library.DirectoryEngine
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.message.IoError
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.service.metadata.CoverExtractionService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExtractCoverFromChapterUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val coverExtractionService: CoverExtractionService,
    @param:DirectoryEngine private val mangaPort: MangaPort<MangaDirectoryDto>,
    @param:DirectoryEngine private val chapterPort: ChapterPort<ChapterArchivePageDto>,
) {
    suspend operator fun invoke(mangaId: Long): Either<LibrarySyncError, Unit> {
        val chapters = chapterPort.observeChapters(mangaId).first().items
        val firstChapter = chapters.minByOrNull { it.chapterSort.toDoubleOrNull() ?: Double.MAX_VALUE }
            ?: return Either.Left(LibrarySyncError.UnexpectedError(Exception("No chapters found to extract cover")))

        val directories = mangaPort.observeLibrary().first()
        val directory = directories.find { it.id == mangaId }
            ?: return Either.Left(LibrarySyncError.UnexpectedError(Exception("Directory not found")))

        val folderUri = directory.path.toUri()
        val folderDoc = DocumentFile.fromTreeUri(context, folderUri)
            ?: return Either.Left(LibrarySyncError.UnexpectedError(Exception("Failed to access folder")))

        return coverExtractionService.extractFirstPageAsCover(folderDoc, firstChapter)
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
