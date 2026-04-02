package br.acerola.manga.service.reader.contract

import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import java.io.InputStream

interface PageSource {

    suspend fun pageCount(): Int
    suspend fun openPage(index: Int): Either<ChapterError, InputStream>
    suspend fun getFileStream(fileName: String): Either<ChapterError, InputStream>
    fun open(chapter: ChapterFileDto): Either<ChapterError, PageSource>
    fun close()
}