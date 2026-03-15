package br.acerola.manga.module.reader.state

import android.graphics.Bitmap
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.dto.archive.ChapterFileDto

enum class TapArea { LEFT, CENTER, RIGHT, BOTTOM, TOP
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
    val pages: Map<Int, Bitmap> = emptyMap(),
    val readingMode: ReadingMode = ReadingMode.HORIZONTAL,
)
