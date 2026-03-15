package br.acerola.manga.__fixtures__

import br.acerola.manga.dto.archive.ChapterFileDto

/**
 * Fixtures reutilizáveis para testes de Mangá.
 * Siga o padrão AAA e use descrições em PT-BR nos testes.
 */
object MangaFixtures {
    fun createChapterFileDto(
        id: Long = 1L,
        name: String = "Capítulo 1",
        path: String = "/path/to/cap1",
        chapterSort: String = "0001"
    ) = ChapterFileDto(
        id = id,
        name = name,
        path = path,
        chapterSort = chapterSort
    )

    fun createChapterList(count: Int = 3): List<ChapterFileDto> {
        return (1..count).map {
            createChapterFileDto(
                id = it.toLong(),
                name = "Capítulo $it",
                chapterSort = it.toString().padStart(4, '0')
            )
        }
    }
}
