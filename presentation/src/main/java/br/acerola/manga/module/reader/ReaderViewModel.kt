package br.acerola.manga.module.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.config.preference.ReadingModePreference
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.module.reader.state.ReaderUiState
import br.acerola.manga.repository.port.HistoryManagementRepository
import br.acerola.manga.service.reader.PageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val repository: PageRepository,
    private val historyRepository: HistoryManagementRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(value = ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            ReadingModePreference.readingModeFlow(context).collect { mode ->
                _state.update { it.copy(readingMode = mode) }
            }
        }
    }

    fun openChapter(mangaId: Long, chapter: ChapterFileDto, initialPage: Int = 0) {
        viewModelScope.launch {
            repository.openChapter(chapter)
                .map {
                    _state.update {
                        it.copy(
                            pageCount = repository.pageCount(),
                            currentPage = initialPage,
                            pages = emptyMap()
                        )
                    }
                }
                .handleResult()
        }
    }

    fun onPageVisible(index: Int) {
        if (state.value.pages.containsKey(index)) return

        viewModelScope.launch {
            repository.loadPage(index).onRight { bitmap ->
                _state.update { it.copy(pages = it.pages + (index to bitmap)) }
            }.handleResult()
        }
    }

    fun onCurrentPageChanged(mangaId: Long, chapterId: Long, index: Int) {
        if (_state.value.currentPage == index) return
        
        _state.update { state ->
            // Cleanup: Mantém 7 páginas (3 acima, 3 abaixo + atual) para evitar flicker
            val newPages = state.pages.filterKeys { it in (index - 3)..(index + 3) }
            state.copy(currentPage = index, pages = newPages)
        }

        viewModelScope.launch {
            historyRepository.upsertHistory(
                ReadingHistoryDto(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    lastPage = index,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        repository.prefetchWindow(center = index, total = state.value.pageCount)
    }

    fun onSliderChanged(index: Int) {
        val target = index.coerceIn(0, state.value.pageCount - 1)
        _state.update { it.copy(currentPage = target) }
    }

    fun toggleUiVisibility() {
        _state.update { it.copy(isUiVisible = !it.isUiVisible) }
    }

    fun updateReadingMode(mode: ReadingMode) {
        viewModelScope.launch {
            ReadingModePreference.saveReadingMode(context, mode)
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            _uiEvents.send(element = error)
        }
    }
}
