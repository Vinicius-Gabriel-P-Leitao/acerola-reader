package br.acerola.comic.fixtures

import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.history.ReadingHistoryDto

/**
 * Fixtures reutilizáveis para testes de Quadrinhos na camada de apresentação.
 */
object MangaFixtures {
    fun createChapterArchivePageDto(
        items: List<ChapterFileDto> = createChapterList(),
        pageSize: Int = 20,
        page: Int = 0,
        total: Int = items.size,
    ) = ChapterPageDto(
        items = items,
        pageSize = pageSize,
        page = page,
        total = total,
    )

    fun createMangaDirectoryDto(
        id: Long = 1L,
        name: String = "Test Comic",
        path: String = "/path/to/comic",
    ) = ComicDirectoryDto(
        id = id,
        name = name,
        path = path,
        coverUri = null,
        bannerUri = null,
        lastModified = 0L,
        archiveTemplateFk = null,
    )

    fun createReadingHistoryDto(
        comicDirectoryId: Long = 1L,
        chapterArchiveId: Long = 10L,
        chapterSort: String = "0001",
        lastPage: Int = 5,
    ) = ReadingHistoryDto(
        comicDirectoryId = comicDirectoryId,
        chapterArchiveId = chapterArchiveId,
        chapterSort = chapterSort,
        lastPage = lastPage,
        isCompleted = false,
        updatedAt = 123456L,
    )

    fun createChapterFileDto(
        id: Long = 1L,
        name: String = "Capítulo 1",
        path: String = "/path/to/cap1",
        chapterSort: String = "0001",
    ) = ChapterFileDto(
        id = id,
        name = name,
        path = path,
        chapterSort = chapterSort,
    )

    fun createChapterList(count: Int = 3): List<ChapterFileDto> =
        (1..count).map {
            createChapterFileDto(
                id = it.toLong(),
                name = "Capítulo $it",
                chapterSort = it.toString().padStart(4, '0'),
            )
        }
}
