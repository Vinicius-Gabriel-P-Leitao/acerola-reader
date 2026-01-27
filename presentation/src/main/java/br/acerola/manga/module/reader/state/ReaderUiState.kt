package br.acerola.manga.module.reader.state

data class ReaderUiState(
    val pageCount: Int = 0,
    val pages: Map<Int, ByteArray> = emptyMap()
)
