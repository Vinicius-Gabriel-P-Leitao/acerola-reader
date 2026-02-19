package br.acerola.manga.service.reader.port

import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import java.io.InputStream

// TODO:  fun open(chapter: ChapterFileDto): ChapterSourceService trazer essa função para essa interface
interface ChapterSourceService {

    suspend fun pageCount(): Int
    suspend fun openPage(index: Int): Either<ChapterError, InputStream>
    suspend fun getFileStream(fileName: String): Either<ChapterError, InputStream>
    fun open(chapter: ChapterFileDto): Either<ChapterError, ChapterSourceService>
    fun close()
}