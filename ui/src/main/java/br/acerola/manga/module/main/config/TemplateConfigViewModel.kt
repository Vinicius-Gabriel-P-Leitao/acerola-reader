package br.acerola.manga.module.main.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.core.usecase.template.AddTemplateUseCase
import br.acerola.manga.core.usecase.template.ObserveTemplatesUseCase
import br.acerola.manga.core.usecase.template.RemoveTemplateUseCase
import br.acerola.manga.local.entity.archive.ChapterTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateConfigViewModel @Inject constructor(
    private val observeTemplates: ObserveTemplatesUseCase,
    private val addTemplate: AddTemplateUseCase,
    private val removeTemplate: RemoveTemplateUseCase
) : ViewModel() {

    val templates: StateFlow<List<ChapterTemplate>> = observeTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onAddTemplate(label: String, pattern: String) {
        viewModelScope.launch {
            addTemplate(label, pattern)
        }
    }

    fun onDeleteTemplate(id: Long) {
        viewModelScope.launch {
            removeTemplate(id)
        }
    }
}
