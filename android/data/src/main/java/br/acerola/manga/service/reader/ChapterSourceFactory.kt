package br.acerola.manga.service.reader

import arrow.core.Either
import arrow.core.left
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import br.acerola.manga.service.reader.extract.CbrPageResolver
import br.acerola.manga.service.reader.extract.CbzPageResolver
import br.acerola.manga.service.reader.contract.PageSource
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

// TODO: Usar um erro traduzido aqui, fazer ele respeitar o meu tratamento de erro
@Singleton
class ChapterSourceFactory @Inject constructor(
    private val cbzProvider: Provider<CbzPageResolver>,
    private val cbrProvider: Provider<CbrPageResolver>
) {

    fun create(chapter: ChapterFileDto): Either<ChapterError, PageSource> {
        return when {
            chapter.path.endsWith(".cbz", true) -> cbzProvider.get().open(chapter)
            chapter.path.endsWith(".cbr", true) -> cbrProvider.get().open(chapter)
            else -> ChapterError.InvalidChapterData("Format not supported: ${chapter.path}").left()
        }
    }
}
