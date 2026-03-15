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
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.module.reader.state.ReaderUiState
import br.acerola.manga.repository.port.HistoryManagementRepository
import br.acerola.manga.service.reader.PageRepository
import br.acerola.manga.usecase.chapter.GetChaptersUseCase
import br.acerola.manga.usecase.di.DirectoryCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import javax.inject.Inject

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
                AcerolaLogger.d(TAG, "Reading mode updated to $mode", LogSource.VIEWMODEL) // LOG SUBSTITUÍDO
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
        AcerolaLogger.i(TAG, "Opening chapter: ${chapter.name} | ID: ${chapter.id}", LogSource.VIEWMODEL) // LOG SUBSTITUÍDO
        _state.update {
            it.copy(
                currentChapter = chapter,
                currentPage = initialPage,
                pages = emptyMap(),
                isLoading = true,
                isChapterRead = false,
                previousChapterId = null,
                nextChapterId = null
            )
        }
        seenPages.clear()

        viewModelScope.launch {
            getChaptersUseCase.observeByManga(mangaId)
                .filter { it.items.isNotEmpty() }
                .take(1)
                .collect { pageDto ->
                    val chapters = pageDto.items.sortedBy { it.chapterSort.toDoubleOrNull() ?: 0.0 }
                    val currentIndex = chapters.indexOfFirst { it.id == chapter.id }
                    
                    if (currentIndex != -1) {
                        val prevChapter = if (currentIndex > 0) chapters[currentIndex - 1] else null
                        val nextChapter = if (currentIndex < chapters.size - 1) chapters[currentIndex + 1] else null
                        
                        AcerolaLogger.d(TAG, "Navigation calculated: prev=${prevChapter?.id ?: "none"}, next=${nextChapter?.id ?: "none"}", LogSource.VIEWMODEL) // LOG SUBSTITUÍDO
                        
                        _state.update { 
                            it.copy(
                                previousChapterId = prevChapter?.id,
                                nextChapterId = nextChapter?.id
                            ) 
                        }
                    }
                }
        }

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
                    AcerolaLogger.d(TAG, "Repository opened. Total pages: ${repository.pageCount()}", LogSource.VIEWMODEL) // LOG SUBSTITUÍDO
                }
                .handleResult()
        }
    }

    fun loadAndOpenChapter(mangaId: Long, chapterId: Long, initialPage: Int = 0) {
        if (_state.value.isLoading) {
            AcerolaLogger.w(TAG, "Blocked loadAndOpenChapter: already loading", LogSource.VIEWMODEL) // LOG SUBSTITUÍDO
            return
        }
        
        AcerolaLogger.i(TAG, "Fetching metadata for chapter ID: $chapterId", LogSource.VIEWMODEL) // LOG SUBSTITUÍDO
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getChaptersUseCase.observeByManga(mangaId)
                .combine(getChaptersUseCase.isIndexing) { pageDto, isIndexing ->
                    pageDto to isIndexing
                }
                .collect { (pageDto, isIndexing) ->
                    val chapter = pageDto.items.find { it.id == chapterId }
                    if (chapter != null) {
                        AcerolaLogger.d(TAG, "Chapter metadata found. Opening...", LogSource.VIEWMODEL) // LOG SUBSTITUÍDO
                        openChapter(mangaId, chapter, initialPage)
                        this@launch.coroutineContext.cancelChildren()
                        return@collect
                    }

                    if (!isIndexing && pageDto.items.isNotEmpty()) {
                        AcerolaLogger.e(TAG, "Chapter ID $chapterId not found locally.", LogSource.VIEWMODEL) // LOG SUBSTITUÍDO
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
            val pageCount = _state.value.pageCount
            val progress = if (pageCount > 0) seenPages.size.toDouble() / pageCount else 0.0

            if (progress >= 0.7) {
                if (!_state.value.isChapterRead) {
                    AcerolaLogger.audit(
                        tag = TAG,
                        msg = "Chapter reached 70% completion",
                        source = LogSource.VIEWMODEL,
                        extras = mapOf("mangaId" to mangaId.toString(), "chapterId" to chapterId.toString())
                    ) // LOG SUBSTITUÍDO
                    _state.update { it.copy(isChapterRead = true) }
                }

                if (!markedAsReadInSession.contains(chapterId)) {
                    viewModelScope.launch {
                        historyRepository.markChapterAsRead(mangaId, chapterId)
                        markedAsReadInSession.add(chapterId)
                    }
                }
            }
        }
    }

    fun onCurrentPageChanged(mangaId: Long, chapterId: Long, index: Int) {
        markPageAsSeen(mangaId, chapterId, index)

        _state.update { it.copy(currentPage = index) }

        val pageCount = _state.value.pageCount
        val isCompletion = pageCount > 0 && index >= pageCount - 1

        if (isCompletion) {
            if (!_state.value.isChapterRead) {
                AcerolaLogger.audit(
                    tag = TAG,
                    msg = "Chapter fully read (reached end)",
                    source = LogSource.VIEWMODEL,
                    extras = mapOf("mangaId" to mangaId.toString(), "chapterId" to chapterId.toString())
                ) // LOG SUBSTITUÍDO
                _state.update { it.copy(isChapterRead = true) }
            }
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

    fun loadNextChapter(mangaId: Long) {
        val nextId = state.value.nextChapterId
        if (nextId != null) {
            AcerolaLogger.audit(TAG, "Transitioning to next chapter", LogSource.UI, mapOf("mangaId" to mangaId.toString(), "nextId" to nextId.toString())) // LOG SUBSTITUÍDO
            loadAndOpenChapter(mangaId, nextId, 0)
        }
    }

    fun loadPreviousChapter(mangaId: Long) {
        val prevId = state.value.previousChapterId
        if (prevId != null) {
            AcerolaLogger.audit(TAG, "Transitioning to previous chapter", LogSource.UI, mapOf("mangaId" to mangaId.toString(), "prevId" to prevId.toString())) // LOG SUBSTITUÍDO
            loadAndOpenChapter(mangaId, prevId, 0)
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            AcerolaLogger.e(TAG, "Reader operation failed: ${error.uiMessage}", LogSource.VIEWMODEL) // LOG SUBSTITUÍDO
            _uiEvents.send(element = error)
            _state.update { it.copy(isLoading = false) }
        }
    }

    companion object {
        private const val TAG = "ReaderViewModel" // PADRÃO OBRIGATÓRIO
    }
}
