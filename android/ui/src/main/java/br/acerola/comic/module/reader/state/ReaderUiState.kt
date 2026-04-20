package br.acerola.comic.module.reader.state
import br.acerola.comic.config.preference.ReadingMode
import br.acerola.comic.dto.archive.ChapterFileDto

enum class TapArea {
    LEFT,
    CENTER,
    RIGHT,
    BOTTOM,
    TOP,
}

data class ReaderUiState(
    val currentChapter: ChapterFileDto? = null,
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val isLoading: Boolean = false,
    val previousChapterId: Long? = null,
    val nextChapterId: Long? = null,
    val isUiVisible: Boolean = true,
    val isChapterRead: Boolean = false,
    val readingMode: ReadingMode = ReadingMode.HORIZONTAL,
)
