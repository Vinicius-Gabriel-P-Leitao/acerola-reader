package br.acerola.comic.module.reader
import br.acerola.comic.ui.R

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.comic.config.preference.ReadingMode
import br.acerola.comic.config.preference.ReadingModePreference
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.error.message.ChapterError
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.module.reader.state.ReaderUiState
import br.acerola.comic.service.reader.ReaderProcessor
import br.acerola.comic.usecase.DirectoryCase
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.history.TrackReadingProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ReaderProcessor,
    @param:ApplicationContext private val context: Context,
    private val trackReadingProgressUseCase: TrackReadingProgressUseCase,
    @param:DirectoryCase private val observeChaptersUseCase: ObserveChaptersUseCase<ChapterArchivePageDto>,
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
                AcerolaLogger.d(TAG, "Reading mode updated to $mode", LogSource.VIEWMODEL)
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

    fun openChapter(
        mangaId: Long,
        chapter: ChapterFileDto,
        initialPage: Int = 0
    ) {
        AcerolaLogger.i(TAG, "Opening chapter: ${chapter.name} | ID: ${chapter.id}", LogSource.VIEWMODEL)
        _state.update {
            it.copy(
                currentChapter = chapter,
                currentPage = initialPage,
                isLoading = true,
                isChapterRead = false,
                previousChapterId = null,
                nextChapterId = null
            )
        }
        seenPages.clear()
        
        viewModelScope.launch {
            observeChaptersUseCase.observeByManga(mangaId)
                .filter { it.items.isNotEmpty() }
                .take(1)
                .collect { pageDto ->
                    val chapters = pageDto.items.sortedBy { it.chapterSort.toDoubleOrNull() ?: 0.0 }
                    val currentIndex = chapters.indexOfFirst { it.id == chapter.id }

                    if (currentIndex != -1) {
                        val prevChapter = if (currentIndex > 0) chapters[currentIndex - 1] else null
                        val nextChapter = if (currentIndex < chapters.size - 1) chapters[currentIndex + 1] else null

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
                            isLoading = false
                        )
                    }
                }
                .handleResult()
        }
    }

    fun loadAndOpenChapter(
        mangaId: Long,
        chapterId: Long,
        initialPage: Int = 0
    ) {
        if (_state.value.isLoading) {
            AcerolaLogger.w(TAG, "Blocked loadAndOpenChapter: already loading", LogSource.VIEWMODEL)
            return
        }

        AcerolaLogger.i(TAG, "Fetching metadata for chapter ID: $chapterId", LogSource.VIEWMODEL)
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            observeChaptersUseCase.observeByManga(mangaId)
                .combine(observeChaptersUseCase.isIndexing) { pageDto, isIndexing ->
                    pageDto to isIndexing
                }
                .collect { (pageDto, isIndexing) ->
                    val chapter = pageDto.items.find { it.id == chapterId }
                    if (chapter != null) {
                        AcerolaLogger.d(TAG, "Chapter metadata found. Opening...", LogSource.VIEWMODEL)
                        openChapter(mangaId, chapter, initialPage)
                        this@launch.coroutineContext.cancelChildren()
                        return@collect
                    }

                    if (!isIndexing && pageDto.items.isNotEmpty()) {
                        AcerolaLogger.e(TAG, "Chapter ID $chapterId not found locally.", LogSource.VIEWMODEL)
                        _uiEvents.send(ChapterError.UnexpectedError(Throwable("Capítulo não encontrado localmente")))
                        _state.update { it.copy(isLoading = false) }
                        this@launch.coroutineContext.cancelChildren()
                        return@collect
                    }
                }
        }
    }

    fun onPageVisible(
        mangaId: Long,
        chapterId: Long,
        index: Int
    ) {
        markPageAsSeen(mangaId, chapterId, index)
    }

    private fun markPageAsSeen(
        mangaId: Long,
        chapterId: Long,
        index: Int
    ) {
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
                    )
                    _state.update { it.copy(isChapterRead = true) }
                }

                if (markedAsReadInSession.add(chapterId)) {
                    viewModelScope.launch {
                        trackReadingProgressUseCase.markChapterAsRead(mangaId, chapterId)
                    }
                }
            }
        }
    }

    fun onCurrentPageChanged(
        mangaId: Long,
        chapterId: Long,
        index: Int
    ) {
        markPageAsSeen(mangaId, chapterId, index)

        _state.update { it.copy(currentPage = index) }

        val pageCount = _state.value.pageCount
        val isLastPage = pageCount > 0 && index >= pageCount - 1

        if (isLastPage) {
            if (!_state.value.isChapterRead) {
                AcerolaLogger.audit(
                    tag = TAG,
                    source = LogSource.VIEWMODEL,
                    msg = "Chapter fully read (reached end)",
                    extras = mapOf("mangaId" to mangaId.toString(), "chapterId" to chapterId.toString())
                )
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

    private suspend fun persistHistory(
        mangaId: Long,
        chapterId: Long,
        page: Int
    ) {
        val pageCount = _state.value.pageCount
        val isLastPage = pageCount > 0 && page >= pageCount - 1
        
        if (isLastPage && markedAsReadInSession.add(chapterId)) {
            trackReadingProgressUseCase.markChapterAsRead(mangaId, chapterId)
        }

        trackReadingProgressUseCase.saveProgress(
            ReadingHistoryDto(
                mangaDirectoryId = mangaId,
                chapterArchiveId = chapterId,
                lastPage = page,
                isCompleted = isLastPage,
                updatedAt = System.currentTimeMillis()
            )
        )
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
            AcerolaLogger.audit(
                TAG, "Transitioning to next chapter", LogSource.UI,
                mapOf("mangaId" to mangaId.toString(), "nextId" to nextId.toString())
            )
            loadAndOpenChapter(mangaId, nextId, 0)
        }
    }

    fun loadPreviousChapter(mangaId: Long) {
        val prevId = state.value.previousChapterId
        if (prevId != null) {
            AcerolaLogger.audit(
                TAG, "Transitioning to previous chapter", LogSource.UI,
                mapOf("mangaId" to mangaId.toString(), "prevId" to prevId.toString())
            )
            loadAndOpenChapter(mangaId, prevId, 0)
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            AcerolaLogger.e(TAG, "Reader operation failed: ${error.uiMessage}", LogSource.VIEWMODEL)
            _uiEvents.send(element = error)
            _state.update { it.copy(isLoading = false) }
        }
    }

    companion object {
        private const val TAG = "ReaderViewModel"
    }
}
