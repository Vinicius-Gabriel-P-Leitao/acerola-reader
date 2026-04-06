package br.acerola.comic.module.main.pattern.state
import br.acerola.comic.ui.R

sealed interface FilePatternAction {
    data class AddTemplate(val label: String, val pattern: String) : FilePatternAction
    data class DeleteTemplate(val id: Long) : FilePatternAction
}
