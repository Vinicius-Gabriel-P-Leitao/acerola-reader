package br.acerola.comic.service.reader

import arrow.core.Either
import arrow.core.left
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.error.message.ChapterError
import br.acerola.comic.service.reader.extract.CbrPageResolver
import br.acerola.comic.service.reader.extract.CbzPageResolver
import br.acerola.comic.service.reader.contract.PageSource
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ChapterSourceFactory @Inject constructor(
    private val cbzProvider: Provider<CbzPageResolver>,
    private val cbrProvider: Provider<CbrPageResolver>
) {

    fun create(chapter: ChapterFileDto): Either<ChapterError, PageSource> {
        return when {
            chapter.path.endsWith(".cbz", true) -> cbzProvider.get().open(chapter)
            chapter.path.endsWith(".cbr", true) -> cbrProvider.get().open(chapter)
            else -> ChapterError.UnsupportedFormat(chapter.path).left()
        }
    }
}
