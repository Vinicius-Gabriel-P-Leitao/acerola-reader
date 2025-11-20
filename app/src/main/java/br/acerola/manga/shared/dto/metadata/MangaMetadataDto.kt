package br.acerola.manga.shared.dto.metadata

data class MangaMetadataDto(
    val id: String,
    val title: String,
    val description: String,
    val romanji: String? = null,
    val gender: List<String> = emptyList(),
    val year: Int? = null,
    val author: String? = null,
)
