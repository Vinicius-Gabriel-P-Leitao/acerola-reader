package br.acerola.comic.module.main.pattern.state
import br.acerola.comic.dto.archive.ArchiveTemplateDto

data class FilePatternUiState(
    val templates: List<ArchiveTemplateDto> = emptyList(),
)
