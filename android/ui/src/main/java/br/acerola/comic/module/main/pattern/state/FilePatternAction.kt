package br.acerola.comic.module.main.pattern.state

import br.acerola.comic.util.sort.SortType

sealed interface FilePatternAction {
    data class AddTemplate(
        val label: String,
        val pattern: String,
        val type: SortType,
    ) : FilePatternAction

    data class EditTemplate(
        val id: Long,
        val label: String,
        val pattern: String,
        val type: SortType,
    ) : FilePatternAction

    data class DeleteTemplate(
        val id: Long,
    ) : FilePatternAction
}
