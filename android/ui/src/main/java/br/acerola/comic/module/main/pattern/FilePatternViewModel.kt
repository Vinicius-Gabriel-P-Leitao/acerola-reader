package br.acerola.comic.module.main.pattern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.comic.error.UserMessage
import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.infra.R
import br.acerola.comic.module.main.pattern.state.FilePatternAction
import br.acerola.comic.module.main.pattern.state.FilePatternUiState
import br.acerola.comic.type.UiText
import br.acerola.comic.usecase.template.AddTemplateUseCase
import br.acerola.comic.usecase.template.ObserveTemplatesUseCase
import br.acerola.comic.usecase.template.RemoveTemplateUseCase
import br.acerola.comic.usecase.template.UpdateTemplateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilePatternViewModel
    @Inject
    constructor(
        private val addTemplate: AddTemplateUseCase,
        private val updateTemplate: UpdateTemplateUseCase,
        private val removeTemplate: RemoveTemplateUseCase,
        private val observeTemplates: ObserveTemplatesUseCase,
    ) : ViewModel() {
        private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
        val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

        val uiState: StateFlow<FilePatternUiState> =
            observeTemplates()
                .map { FilePatternUiState(templates = it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = FilePatternUiState(),
                )

        fun onAction(action: FilePatternAction) {
            when (action) {
                is FilePatternAction.AddTemplate -> addTemplate(action.label, action.pattern)
                is FilePatternAction.EditTemplate -> editTemplate(action.id, action.label, action.pattern)
                is FilePatternAction.DeleteTemplate -> deleteTemplate(action.id)
            }
        }

        private fun addTemplate(
            label: String,
            pattern: String,
        ) {
            viewModelScope.launch {
                when (val result = addTemplate.invoke(label, pattern)) {
                    is Either.Left -> _uiEvents.send(UserMessage.Raw(result.value.toUiText()))
                    is Either.Right -> Unit
                }
            }
        }

        private fun editTemplate(
            id: Long,
            label: String,
            pattern: String,
        ) {
            viewModelScope.launch {
                when (val result = updateTemplate.invoke(id, label, pattern)) {
                    is Either.Left -> _uiEvents.send(UserMessage.Raw(result.value.toUiText()))
                    is Either.Right -> Unit
                }
            }
        }

        private fun deleteTemplate(id: Long) {
            viewModelScope.launch {
                removeTemplate.invoke(id)
            }
        }

        private fun TemplateError.toUiText(): UiText =
            when (this) {
                is TemplateError.InvalidPattern -> uiMessage
                TemplateError.Duplicate -> UiText.StringResource(R.string.error_template_duplicate)
                TemplateError.SystemProtected -> UiText.StringResource(R.string.error_template_system_protected)
            }
    }
