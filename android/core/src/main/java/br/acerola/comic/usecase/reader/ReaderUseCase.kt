package br.acerola.comic.usecase.reader

import android.graphics.Bitmap
import arrow.core.Either
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.error.message.ChapterError
import br.acerola.comic.service.reader.ReaderProcessor
import javax.inject.Inject

class ReaderUseCase @Inject constructor(
    private val processor: ReaderProcessor,
) {
    fun openChapter(chapter: ChapterFileDto): Either<ChapterError, Unit> =
        processor.openChapter(chapter)

    suspend fun pageCount(): Int = processor.pageCount()

    suspend fun loadPage(index: Int): Either<ChapterError, Bitmap> =
        processor.loadPage(index)

    fun prefetchWindow(center: Int, total: Int) =
        processor.prefetchWindow(center, total)
}
