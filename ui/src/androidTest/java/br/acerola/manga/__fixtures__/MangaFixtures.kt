package br.acerola.manga.__fixtures__

import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.dto.ChapterDto
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.module.manga.state.MainTab
import br.acerola.manga.module.manga.state.MangaUiState

/**
 * Fixtures reutilizáveis para testes de Mangá na camada de apresentação.
 */
object MangaFixtures {
    fun createMangaUiState(
        manga: MangaDto = MangaDto(directory = createMangaDirectoryDto(), remoteInfo = null),
        chapters: ChapterDto? = null,
        selectedTab: MainTab = MainTab.CHAPTERS,
        isIndexing: Boolean = false,
        indexingProgress: Float? = null,
        history: ReadingHistoryDto? = null,
        readChapters: Set<Long> = emptySet(),
        totalChapters: Int = 0,
        currentPage: Int = 0,
        totalPages: Int = 0,
        selectedChapterPerPage: ChapterPageSizeType = ChapterPageSizeType.SHORT
    ) = MangaUiState(
        manga = manga,
        chapters = chapters,
        selectedTab = selectedTab,
        isIndexing = isIndexing,
        indexingProgress = indexingProgress,
        history = history,
        readChapters = readChapters,
        totalChapters = totalChapters,
        currentPage = currentPage,
        totalPages = totalPages,
        selectedChapterPerPage = selectedChapterPerPage
    )

    fun createChapterArchivePageDto(
        items: List<ChapterFileDto> = createChapterList(),
        pageSize: Int = 20,
        page: Int = 0,
        total: Int = items.size
    ) = ChapterArchivePageDto(
        items = items,
        pageSize = pageSize,
        page = page,
        total = total
    )

    fun createMangaDirectoryDto(
        id: Long = 1L,
        name: String = "Test Manga",
        path: String = "/path/to/manga"
    ) = MangaDirectoryDto(
        id = id,
        name = name,
        path = path,
        coverUri = null,
        bannerUri = null,
        lastModified = 0L,
        chapterTemplateFk = null
    )

    fun createReadingHistoryDto(
        mangaDirectoryId: Long = 1L,
        chapterArchiveId: Long = 10L,
        lastPage: Int = 5
    ) = ReadingHistoryDto(
        mangaDirectoryId = mangaDirectoryId,
        chapterArchiveId = chapterArchiveId,
        lastPage = lastPage,
        isCompleted = false,
        updatedAt = 123456L
    )

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
