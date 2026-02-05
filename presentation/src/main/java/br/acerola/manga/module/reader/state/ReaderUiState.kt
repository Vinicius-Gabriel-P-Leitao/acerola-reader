package br.acerola.manga.module.reader.state

enum class TapArea { LEFT, CENTER, RIGHT, BOTTOM, TOP
}

enum class ReadingMode { HORIZONTAL, VERTICAL, WEBTOON
}

data class ReaderUiState(
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val isUiVisible: Boolean = true,
    val pages: Map<Int, ByteArray> = emptyMap(),
    val readingMode: ReadingMode = ReadingMode.HORIZONTAL,
)