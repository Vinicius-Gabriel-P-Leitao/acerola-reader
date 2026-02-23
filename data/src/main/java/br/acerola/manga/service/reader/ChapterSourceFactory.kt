package br.acerola.manga.service.reader

import arrow.core.Either
import arrow.core.left
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import br.acerola.manga.service.reader.extract.CbrChapterSourceService
import br.acerola.manga.service.reader.extract.CbzChapterSourceService
import br.acerola.manga.service.reader.port.ChapterSourceService
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

// TODO: Não existe muito o que otimizar aqui além do erro para formato invalido
@Singleton
class ChapterSourceFactory @Inject constructor(
    private val cbzProvider: Provider<CbzChapterSourceService>,
    private val cbrProvider: Provider<CbrChapterSourceService>
) {

    fun create(chapter: ChapterFileDto): Either<ChapterError, ChapterSourceService> {
        return when {
            chapter.path.endsWith(".cbz", true) -> cbzProvider.get().open(chapter)
            chapter.path.endsWith(".cbr", true) -> cbrProvider.get().open(chapter)
            else -> ChapterError.InvalidChapterData("Format not supported: ${chapter.path}").left()
        }
    }
}
