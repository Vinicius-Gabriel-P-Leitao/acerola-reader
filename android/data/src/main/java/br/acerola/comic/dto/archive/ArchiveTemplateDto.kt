package br.acerola.comic.dto.archive

import br.acerola.comic.util.sort.SortType

data class ArchiveTemplateDto(
    val id: Long = 0,
    val label: String,
    val pattern: String,
    val type: SortType,
    val isDefault: Boolean = false,
    val priority: Int = 0,
)
