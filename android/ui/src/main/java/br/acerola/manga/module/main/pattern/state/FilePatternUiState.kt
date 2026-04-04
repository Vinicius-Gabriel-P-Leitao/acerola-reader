package br.acerola.manga.module.main.pattern.state

import br.acerola.manga.dto.archive.ChapterTemplateDto

data class FilePatternUiState(
    val templates: List<ChapterTemplateDto> = emptyList()
)
