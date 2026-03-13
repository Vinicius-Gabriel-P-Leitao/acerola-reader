package br.acerola.manga.module.reader.state

import android.graphics.Bitmap
import br.acerola.manga.config.preference.ReadingMode

enum class TapArea { LEFT, CENTER, RIGHT, BOTTOM, TOP
}

data class ReaderUiState(
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val isLoading: Boolean = false,
    val isUiVisible: Boolean = true,
    val pages: Map<Int, Bitmap> = emptyMap(),
    val readingMode: ReadingMode = ReadingMode.HORIZONTAL,
)