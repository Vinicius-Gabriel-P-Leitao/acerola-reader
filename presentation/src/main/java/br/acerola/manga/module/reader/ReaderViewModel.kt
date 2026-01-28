package br.acerola.manga.module.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.module.reader.state.ReaderUiState
import br.acerola.manga.service.reader.PageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Criar método que usa o maldito UserMessage para fazer Toast de erros para o usuário
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: PageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()


    fun openChapter(chapter: ChapterFileDto) {
        viewModelScope.launch {
            repository.openChapter(chapter)
                .map {
                    _state.update {
                        it.copy(
                            pageCount = repository.pageCount(),
                            pages = emptyMap()
                        )
                    }
                }
                .handleResult()
        }
    }

    fun onPageVisible(index: Int) {
        viewModelScope.launch {
            repository.loadPage(index).map { page ->
                    _state.update {
                        it.copy(pages = it.pages + (index to page))
                    }
                }.handleResult()

            repository.prefetchWindow(center = index)
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            _uiEvents.send(element = error)
        }
    }
}
