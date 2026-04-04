package br.acerola.manga.dto.archive

data class ChapterTemplateDto(
    val id: Long = 0,
    val label: String,
    val pattern: String,
    val isDefault: Boolean = false,
    val priority: Int = 0
)
