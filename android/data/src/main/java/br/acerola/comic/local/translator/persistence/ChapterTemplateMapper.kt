package br.acerola.comic.local.translator.persistence

import br.acerola.comic.dto.archive.ChapterTemplateDto
import br.acerola.comic.local.entity.archive.ChapterTemplate

fun ChapterTemplate.toDto() =
    ChapterTemplateDto(
        id = id,
        label = label,
        pattern = pattern,
        isDefault = isDefault,
        priority = priority,
    )
