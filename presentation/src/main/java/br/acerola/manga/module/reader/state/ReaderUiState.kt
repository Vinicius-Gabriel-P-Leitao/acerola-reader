package br.acerola.manga.module.reader.state

enum class ReadingMode {
    PAGINATED,
    VERTICAL,
    WEBTOON
}

data class ReaderUiState(
    val pageCount: Int = 0,
    val pages: Map<Int, ByteArray> = emptyMap(),
    val currentPage: Int = 0,
    val isUiVisible: Boolean = true,
    val readingMode: ReadingMode = ReadingMode.PAGINATED,
)