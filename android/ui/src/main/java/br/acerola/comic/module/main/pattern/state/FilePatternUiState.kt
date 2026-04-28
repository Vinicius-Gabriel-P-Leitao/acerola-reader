package br.acerola.comic.module.main.pattern.state
import br.acerola.comic.dto.archive.ChapterTemplateDto

data class FilePatternUiState(
    val templates: List<ChapterTemplateDto> = emptyList(),
)
