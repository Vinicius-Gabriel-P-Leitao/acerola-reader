package br.acerola.manga.module.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.config.preference.ReadingModePreference
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.error.message.ChapterError
import br.acerola.manga.module.reader.state.ReaderUiState
import br.acerola.manga.repository.port.HistoryManagementRepository
import br.acerola.manga.service.reader.PageRepository
import br.acerola.manga.usecase.chapter.GetChaptersUseCase
import br.acerola.manga.usecase.di.DirectoryCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import javax.inject.Inject

// TODO: Criar método que usa o maldito UserMessage para fazer Toast de erros para o usuário
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: PageRepository,
    private val historyRepository: HistoryManagementRepository,
    @DirectoryCase private val getChaptersUseCase: GetChaptersUseCase<ChapterArchivePageDto>,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(value = ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _historyUpdates = Channel<Triple<Long, Long, Int>>(Channel.BUFFERED)
    private val seenPages = mutableSetOf<Int>()
    private val markedAsReadInSession = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            ReadingModePreference.readingModeFlow(context).collect { mode ->
                _state.update { it.copy(readingMode = mode) }
            }
        }
        viewModelScope.launch {
            _historyUpdates.receiveAsFlow()
                .collect { (mId, cId, idx) ->
                    persistHistory(mId, cId, idx)
                }
        }
    }

    fun openChapter(mangaId: Long, chapter: ChapterFileDto, initialPage: Int = 0) {
        _state.update { it.copy(currentPage = initialPage, pages = emptyMap(), isLoading = true) }
        seenPages.clear()
        // Não limpamos markedAsReadInSession para evitar spam se o usuário alternar capítulos
        viewModelScope.launch {
            repository.openChapter(chapter)
                .map {
                    _state.update {
                        it.copy(
                            pageCount = repository.pageCount(),
                            currentPage = initialPage,
                            pages = emptyMap(),
                            isLoading = false
                        )
                    }
                }
                .handleResult()
        }
    }

    fun loadAndOpenChapter(mangaId: Long, chapterId: Long, initialPage: Int = 0) {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Usa transformWhile para coletar até encontrar o capítulo ou desistir
            getChaptersUseCase.observeByManga(mangaId)
                .combine(getChaptersUseCase.isIndexing) { pageDto, isIndexing ->
                    pageDto to isIndexing
                }
                .collect { (pageDto, isIndexing) ->
                    val chapter = pageDto.items.find { it.id == chapterId }
                    if (chapter != null) {
                        openChapter(mangaId, chapter, initialPage)
                        this@launch.coroutineContext.cancelChildren() // Para a coleta
                        return@collect
                    }
                    
                    if (!isIndexing && pageDto.items.isNotEmpty()) {
                        // Se terminou de indexar e não achou
                        _uiEvents.send(ChapterError.UnexpectedError(Throwable("Capítulo não encontrado localmente")))
                        _state.update { it.copy(isLoading = false) }
                        this@launch.coroutineContext.cancelChildren()
                        return@collect
                    }
                }
        }
    }

    fun onPageVisible(mangaId: Long, chapterId: Long, index: Int) {
        if (state.value.pages.containsKey(index)) {
            markPageAsSeen(mangaId, chapterId, index)
            return
        }

        viewModelScope.launch {
            repository.loadPage(index).onRight { bitmap ->
                markPageAsSeen(mangaId, chapterId, index)
                _state.update { it.copy(pages = it.pages + (index to bitmap)) }
            }.handleResult()
        }
    }

    private fun markPageAsSeen(mangaId: Long, chapterId: Long, index: Int) {
        if (seenPages.add(index)) {
            // Se uma nova página foi vista, verificamos se o threshold de 70% foi atingido
            val pageCount = _state.value.pageCount
            val progress = if (pageCount > 0) seenPages.size.toDouble() / pageCount else 0.0

            if (progress >= 0.7 && !markedAsReadInSession.contains(chapterId)) {
                viewModelScope.launch {
                    historyRepository.markChapterAsRead(mangaId, chapterId)
                    markedAsReadInSession.add(chapterId)
                }
            }
        }
    }

    fun onCurrentPageChanged(mangaId: Long, chapterId: Long, index: Int) {
        markPageAsSeen(mangaId, chapterId, index)

        _state.update { state ->
            state.copy(currentPage = index)
        }

        val pageCount = _state.value.pageCount
        val isCompletion = pageCount > 0 && index >= pageCount - 1

        if (isCompletion) {
            // Salva imediatamente se for conclusão
            viewModelScope.launch {
                persistHistory(mangaId, chapterId, index)
            }
        } else {
            _historyUpdates.trySend(Triple(mangaId, chapterId, index))
        }

        repository.prefetchWindow(center = index, total = state.value.pageCount)
    }

    private suspend fun persistHistory(mangaDirectoryId: Long, chapterArchiveId: Long, index: Int) {
        historyRepository.upsertHistory(
            ReadingHistoryDto(
                mangaDirectoryId = mangaDirectoryId,
                chapterArchiveId = chapterArchiveId,
                lastPage = index,
                isCompleted = false,
                updatedAt = System.currentTimeMillis()
            )
        )

        // A marcação como lido já é tratada pelo markPageAsSeen se atingir 70%
        // Mas se atingiu a última página, garantimos aqui também
        val pageCount = _state.value.pageCount
        if (pageCount > 0 && index >= pageCount - 1 && !markedAsReadInSession.contains(chapterArchiveId)) {
            historyRepository.markChapterAsRead(mangaDirectoryId, chapterArchiveId)
            markedAsReadInSession.add(chapterArchiveId)
        }
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
            _state.update { it.copy(isLoading = false) }
        }
    }
}
