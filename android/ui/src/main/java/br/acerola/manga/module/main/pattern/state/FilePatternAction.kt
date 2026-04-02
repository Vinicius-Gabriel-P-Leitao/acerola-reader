package br.acerola.manga.module.main.pattern.state

sealed interface FilePatternAction {
    data class AddTemplate(val label: String, val pattern: String) : FilePatternAction
    data class DeleteTemplate(val id: Long) : FilePatternAction
}
