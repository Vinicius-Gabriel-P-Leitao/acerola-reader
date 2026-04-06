package br.acerola.comic.__fixtures__

import br.acerola.comic.config.preference.ChapterPageSizeType
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.module.comic.state.ComicUiState
import br.acerola.comic.module.comic.state.MainTab
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Fixtures reutilizáveis para testes de Mangá na camada de apresentação.
 */
object ComicFixtures {
    fun createMangaUiState(
        manga: ComicDto = ComicDto(directory = createMangaDirectoryDto(), remoteInfo = null),
        chapters: ChapterDto? = null,
        selectedTab: MainTab = MainTab.CHAPTERS,
        history: ReadingHistoryDto? = null,
        readChapters: PersistentSet<Long> = persistentSetOf(),
        totalChapters: Int = 0,
        currentPage: Int = 0,
        totalPages: Int = 0,
        selectedChapterPerPage: ChapterPageSizeType = ChapterPageSizeType.SHORT,
        allCategories: PersistentList<CategoryDto> = persistentListOf()
    ) = ComicUiState(
        manga = manga,
        chapters = chapters,
        selectedTab = selectedTab,
        history = history,
        readChapters = readChapters,
        totalChapters = totalChapters,
        currentPage = currentPage,
        totalPages = totalPages,
        selectedChapterPerPage = selectedChapterPerPage,
        allCategories = allCategories
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
        path: String = "/path/to/comic"
    ) = ComicDirectoryDto(
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
