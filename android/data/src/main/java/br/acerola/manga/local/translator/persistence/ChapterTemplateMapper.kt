package br.acerola.manga.local.translator.persistence

import br.acerola.manga.dto.archive.ChapterTemplateDto
import br.acerola.manga.local.entity.archive.ChapterTemplate

fun ChapterTemplate.toDto() = ChapterTemplateDto(
    id = id,
    label = label,
    pattern = pattern,
    isDefault = isDefault,
    priority = priority
)
