package br.acerola.comic.service.reader.contract

import arrow.core.Either
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.error.message.ChapterError
import java.io.InputStream

interface PageSource {

    suspend fun pageCount(): Int
    suspend fun openPage(index: Int): Either<ChapterError, InputStream>
    suspend fun getFileStream(fileName: String): Either<ChapterError, InputStream>
    fun open(chapter: ChapterFileDto): Either<ChapterError, PageSource>
    fun close()
}