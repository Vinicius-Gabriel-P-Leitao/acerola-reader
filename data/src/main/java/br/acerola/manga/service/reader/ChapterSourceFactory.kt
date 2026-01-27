package br.acerola.manga.service.reader

import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.service.reader.cbr.CbrChapterSourceService
import br.acerola.manga.service.reader.cbz.CbzChapterSourceService
import br.acerola.manga.service.reader.port.ChapterSourceService
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Não existe muito o que otimizar aqui além do erro para formato invalido
@Singleton
class ChapterSourceFactory @Inject constructor(
    private val cbz: CbzChapterSourceService,
    private val cbr: CbrChapterSourceService
) {

    fun create(chapter: ChapterFileDto): ChapterSourceService {
        return when {
            chapter.path.endsWith(".cbz", true) -> cbz.open(chapter)
            chapter.path.endsWith(".cbr", true) -> cbr.open(chapter)
            else -> error("Formato não suportado")
        }
    }
}