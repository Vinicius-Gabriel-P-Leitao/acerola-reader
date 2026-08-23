package br.acerola.comic.local.translator.persistence

import br.acerola.comic.dto.archive.ArchiveTemplateDto
import br.acerola.comic.local.entity.archive.ArchiveTemplate

fun ArchiveTemplate.toDto() =
    ArchiveTemplateDto(
        id = id,
        label = label,
        pattern = pattern,
        type = type,
        isDefault = isDefault,
        priority = priority,
    )
