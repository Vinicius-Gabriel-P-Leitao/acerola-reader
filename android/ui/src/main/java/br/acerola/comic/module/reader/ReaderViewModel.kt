package br.acerola.comic.module.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.comic.config.preference.ReadingModePreference
import br.acerola.comic.config.preference.types.ReadingMode
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.error.message.ChapterError
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.module.reader.state.ReaderUiState
import br.acerola.comic.usecase.DirectoryCase
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.history.TrackReadingProgressUseCase
import br.acerola.comic.usecase.reader.ReaderUseCase
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
class ReaderViewModel
    @Inject
    constructor(
        private val readerUseCase: ReaderUseCase,
        @param:ApplicationContext private val context: Context,
        private val trackReadingProgressUseCase: TrackReadingProgressUseCase,
        @param:DirectoryCase private val observeChaptersUseCase: ObserveChaptersUseCase<ChapterPageDto>,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(value = ReaderUiState())
        val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

        private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
        val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

        private val historyUpdates = Channel<Triple<Long, String, Int>>(Channel.BUFFERED)
        private val seenPages = mutableSetOf<Int>()
        private val markedAsReadInSession = mutableSetOf<String>()

        init {
            viewModelScope.launch {
                ReadingModePreference.readingModeFlow(context).collect { mode ->
                    AcerolaLogger.d(TAG, "Reading mode updated to $mode", LogSource.VIEWMODEL)
                    _uiState.update { it.copy(readingMode = mode) }
                }
            }
            viewModelScope.launch {
                historyUpdates
                    .receiveAsFlow()
                    .collect { (mId, cSort, idx) ->
                        persistHistory(mId, cSort, idx)
                    }
            }
        }

        fun openChapter(
            comicId: Long,
            chapter: ChapterFileDto,
            initialPage: Int = 0,
        ) {
            AcerolaLogger.i(TAG, "Opening chapter: ${chapter.name} | Sort: ${chapter.chapterSort}", LogSource.VIEWMODEL)
            _uiState.update {
                it.copy(
                    currentChapter = chapter,
                    currentPage = initialPage,
                    isLoading = true,
                    isChapterRead = false,
                    previousChapterId = null,
                    nextChapterId = null,
                )
            }
            seenPages.clear()

            viewModelScope.launch {
                observeChaptersUseCase
                    .observeByComic(comicId)
                    .filter { it.items.isNotEmpty() }
                    .take(1)
                    .collect { pageDto ->
                        val chapters = pageDto.items
                        val currentIndex = chapters.indexOfFirst { it.chapterSort == chapter.chapterSort }

                        if (currentIndex != -1) {
                            val prevChapter = if (currentIndex > 0) chapters[currentIndex - 1] else null
                            val nextChapter = if (currentIndex < chapters.size - 1) chapters[currentIndex + 1] else null

                            _uiState.update {
                                it.copy(
                                    previousChapterId = prevChapter?.id,
                                    nextChapterId = nextChapter?.id,
                                )
                            }
                        }
                    }
            }

            viewModelScope.launch {
                readerUseCase
                    .openChapter(chapter)
                    .map {
                        _uiState.update {
                            it.copy(
                                pageCount = readerUseCase.pageCount(),
                                currentPage = initialPage,
                                isLoading = false,
                            )
                        }
                    }.handleResult()
            }
        }

        fun loadAndOpenChapter(
            comicId: Long,
            chapterId: Long?,
            chapterSort: String,
            initialPage: Int = 0,
        ) {
            if (_uiState.value.isLoading) {
                AcerolaLogger.w(TAG, "Blocked loadAndOpenChapter: already loading", LogSource.VIEWMODEL)
                return
            }

            AcerolaLogger.i(TAG, "Fetching metadata for chapter Sort: $chapterSort", LogSource.VIEWMODEL)
            _uiState.update { it.copy(isLoading = true) }

            viewModelScope.launch {
                observeChaptersUseCase
                    .observeByComic(comicId)
                    .combine(observeChaptersUseCase.isIndexing) { pageDto, isIndexing ->
                        pageDto to isIndexing
                    }.collect { (pageDto, isIndexing) ->
                        val chapter =
                            pageDto.items.find {
                                if (chapterId != null && it.id == chapterId) {
                                    true
                                } else {
                                    it.chapterSort == chapterSort
                                }
                            }
                        if (chapter != null) {
                            AcerolaLogger.d(TAG, "Chapter metadata found. Opening...", LogSource.VIEWMODEL)
                            openChapter(comicId, chapter, initialPage)
                            this@launch.coroutineContext.cancelChildren()
                            return@collect
                        }

                        if (!isIndexing && pageDto.items.isNotEmpty()) {
                            AcerolaLogger.e(TAG, "Chapter $chapterSort not found locally.", LogSource.VIEWMODEL)
                            _uiEvents.send(ChapterError.UnexpectedError(Throwable("Capítulo não encontrado localmente")))
                            _uiState.update { it.copy(isLoading = false) }
                            this@launch.coroutineContext.cancelChildren()
                            return@collect
                        }
                    }
            }
        }

        fun onPageVisible(
            comicId: Long,
            chapterSort: String,
            chapterId: Long?,
            index: Int,
        ) {
            markPageAsSeen(comicId, chapterSort, chapterId, index)
        }

        private fun markPageAsSeen(
            comicId: Long,
            chapterSort: String,
            chapterId: Long?,
            index: Int,
        ) {
            if (seenPages.add(index)) {
                val pageCount = _uiState.value.pageCount
                val progress = if (pageCount > 0) seenPages.size.toDouble() / pageCount else 0.0

                if (progress >= 0.7) {
                    if (!_uiState.value.isChapterRead) {
                        AcerolaLogger.audit(
                            tag = TAG,
                            msg = "Chapter reached 70% completion",
                            source = LogSource.VIEWMODEL,
                            extras = mapOf("comicId" to comicId.toString(), "chapterSort" to chapterSort),
                        )
                        _uiState.update { it.copy(isChapterRead = true) }
                    }

                    if (markedAsReadInSession.add(chapterSort)) {
                        viewModelScope.launch {
                            trackReadingProgressUseCase.markChapterAsRead(comicId, chapterSort, chapterId)
                        }
                    }
                }
            }
        }

        fun onCurrentPageChanged(
            comicId: Long,
            chapterSort: String,
            chapterId: Long?,
            index: Int,
        ) {
            markPageAsSeen(comicId, chapterSort, chapterId, index)

            _uiState.update { it.copy(currentPage = index) }

            val pageCount = _uiState.value.pageCount
            val isLastPage = pageCount > 0 && index >= pageCount - 1

            if (isLastPage) {
                if (!_uiState.value.isChapterRead) {
                    AcerolaLogger.audit(
                        tag = TAG,
                        source = LogSource.VIEWMODEL,
                        msg = "Chapter fully read (reached end)",
                        extras = mapOf("comicId" to comicId.toString(), "chapterSort" to chapterSort),
                    )
                    _uiState.update { it.copy(isChapterRead = true) }
                }
                viewModelScope.launch {
                    persistHistory(comicId, chapterSort, index)
                }
            } else {
                historyUpdates.trySend(Triple(comicId, chapterSort, index))
            }

            readerUseCase.prefetchWindow(center = index, total = uiState.value.pageCount)
        }

        private suspend fun persistHistory(
            comicId: Long,
            chapterSort: String,
            page: Int,
        ) {
            val pageCount = _uiState.value.pageCount
            val isLastPage = pageCount > 0 && page >= pageCount - 1

            val chapterId = _uiState.value.currentChapter?.id

            if (isLastPage && markedAsReadInSession.add(chapterSort)) {
                trackReadingProgressUseCase.markChapterAsRead(comicId, chapterSort, chapterId)
            }

            trackReadingProgressUseCase.saveProgress(
                ReadingHistoryDto(
                    comicDirectoryId = comicId,
                    chapterArchiveId = chapterId,
                    chapterSort = chapterSort,
                    lastPage = page,
                    isCompleted = isLastPage,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        fun onSliderChanged(index: Int) {
            val target = index.coerceIn(0, uiState.value.pageCount - 1)
            _uiState.update { it.copy(currentPage = target) }
        }

        fun toggleUiVisibility() {
            _uiState.update { it.copy(isUiVisible = !it.isUiVisible) }
        }

        fun updateReadingMode(mode: ReadingMode) {
            viewModelScope.launch {
                ReadingModePreference.saveReadingMode(context, mode)
            }
        }

        fun loadNextChapter(comicId: Long) {
            val nextId = uiState.value.nextChapterId
            if (nextId != null) {
                AcerolaLogger.audit(
                    TAG,
                    "Transitioning to next chapter",
                    LogSource.UI,
                    mapOf("comicId" to comicId.toString(), "nextId" to nextId.toString()),
                )

                loadAndOpenChapter(comicId, nextId, "", 0)
            }
        }

        fun loadPreviousChapter(comicId: Long) {
            val prevId = uiState.value.previousChapterId
            if (prevId != null) {
                AcerolaLogger.audit(
                    TAG,
                    "Transitioning to previous chapter",
                    LogSource.UI,
                    mapOf("comicId" to comicId.toString(), "prevId" to prevId.toString()),
                )
                loadAndOpenChapter(comicId, prevId, "", 0)
            }
        }

        private suspend fun <T> Either<UserMessage, T>.handleResult() {
            this.onLeft { error ->
                AcerolaLogger.e(TAG, "Reader operation failed: ${error.uiMessage}", LogSource.VIEWMODEL)
                _uiEvents.send(element = error)
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        companion object {
            private const val TAG = "ReaderViewModel"
        }
    }
