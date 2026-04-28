package br.acerola.comic.module.main.pattern.state

sealed interface FilePatternAction {
    data class AddTemplate(
        val label: String,
        val pattern: String,
    ) : FilePatternAction

    data class EditTemplate(
        val id: Long,
        val label: String,
        val pattern: String,
    ) : FilePatternAction

    data class DeleteTemplate(
        val id: Long,
    ) : FilePatternAction
}
