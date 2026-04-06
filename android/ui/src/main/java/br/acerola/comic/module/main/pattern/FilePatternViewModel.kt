package br.acerola.comic.module.main.pattern
import br.acerola.comic.ui.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.comic.usecase.template.AddTemplateUseCase
import br.acerola.comic.usecase.template.ObserveTemplatesUseCase
import br.acerola.comic.usecase.template.RemoveTemplateUseCase
import br.acerola.comic.module.main.pattern.state.FilePatternAction
import br.acerola.comic.module.main.pattern.state.FilePatternUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilePatternViewModel @Inject constructor(
    private val addTemplate: AddTemplateUseCase,
    private val removeTemplate: RemoveTemplateUseCase,
    private val observeTemplates: ObserveTemplatesUseCase,
) : ViewModel() {

    val uiState: StateFlow<FilePatternUiState> = observeTemplates()
        .map { FilePatternUiState(templates = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FilePatternUiState()
        )

    fun onAction(action: FilePatternAction) {
        when (action) {
            is FilePatternAction.AddTemplate -> addTemplate(action.label, action.pattern)
            is FilePatternAction.DeleteTemplate -> deleteTemplate(action.id)
        }
    }

    private fun addTemplate(label: String, pattern: String) {
        viewModelScope.launch {
            addTemplate.invoke(label, pattern)
        }
    }

    private fun deleteTemplate(id: Long) {
        viewModelScope.launch {
            removeTemplate.invoke(id)
        }
    }
}
