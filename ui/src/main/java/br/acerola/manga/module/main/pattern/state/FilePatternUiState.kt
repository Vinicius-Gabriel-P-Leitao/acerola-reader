package br.acerola.manga.module.main.pattern.state

import br.acerola.manga.local.entity.archive.ChapterTemplate

data class FilePatternUiState(
    val templates: List<ChapterTemplate> = emptyList()
)
