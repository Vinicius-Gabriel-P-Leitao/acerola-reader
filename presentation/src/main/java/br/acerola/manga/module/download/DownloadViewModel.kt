package br.acerola.manga.module.download

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.acerola.manga.config.pattern.ArchiveFormatPattern
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.module.download.state.DownloadAction
import br.acerola.manga.module.download.state.DownloadUiState
import br.acerola.manga.presentation.R
import br.acerola.manga.service.worker.ChapterDownloadWorker
import br.acerola.manga.usecase.search.SearchMangaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val searchMangaUseCase: SearchMangaUseCase,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    fun init(manga: MangaRemoteInfoDto) {
        if (_uiState.value.manga?.mangadexId == manga.mangadexId) return
        _uiState.update { it.copy(manga = manga, isLoadingChapters = true) }

        val mangadexId = manga.mangadexId ?: return
        loadChapters(mangadexId, _uiState.value.selectedLanguage, page = 0)
    }

    fun onAction(action: DownloadAction) {
        when (action) {
            is DownloadAction.SelectLanguage -> onLanguageSelected(action.language)
            is DownloadAction.ToggleChapter -> toggleChapter(action.chapterId)
            is DownloadAction.ChangePage -> changePage(action.page)
            is DownloadAction.SelectAll -> selectAll()
            is DownloadAction.DeselectAll -> _uiState.update { it.copy(selectedChapterIds = emptySet()) }
            is DownloadAction.Download -> enqueueDownload()
            is DownloadAction.DownloadAll -> downloadAll()
        }
    }

    private fun onLanguageSelected(language: String) {
        val mangaId = _uiState.value.manga?.mangadexId ?: return
        _uiState.update {
            it.copy(
                selectedLanguage = language,
                isLoadingChapters = true,
                chapters = emptyList(),
                allSeenChapters = emptyMap(),
                selectedChapterIds = emptySet(),
                totalChapters = 0,
                currentPage = 0,
            )
        }
        loadChapters(mangaId, language, page = 0)
    }

    private fun changePage(page: Int) {
        val state = _uiState.value
        val mangaId = state.manga?.mangadexId ?: return
        _uiState.update { it.copy(isLoadingChapters = true, chapters = emptyList(), currentPage = page) }
        loadChapters(mangaId, state.selectedLanguage, page = page)
    }

    private fun loadChapters(mangaId: String, language: String, page: Int) {
        val limit = _uiState.value.chaptersPerPage
        viewModelScope.launch {
            searchMangaUseCase.getChaptersByLanguage(mangaId, language, page, limit).fold(
                ifLeft = { error ->
                    _uiState.update { it.copy(isLoadingChapters = false) }
                    _uiEvents.send(UserMessage.Raw(error.uiMessage.asString(context)))
                },
                ifRight = { (chapters, total) ->
                    _uiState.update { state ->
                        state.copy(
                            isLoadingChapters = false,
                            chapters = chapters,
                            allSeenChapters = state.allSeenChapters + chapters.associateBy { it.id },
                            totalChapters = total,
                            currentPage = page,
                        )
                    }
                }
            )
        }
    }

    private fun toggleChapter(chapterId: String) {
        _uiState.update { state ->
            val updated = if (chapterId in state.selectedChapterIds) {
                state.selectedChapterIds - chapterId
            } else {
                state.selectedChapterIds + chapterId
            }
            state.copy(selectedChapterIds = updated)
        }
    }

    // Accumulates selections across pages — never replaces previous pages' selections
    private fun selectAll() {
        _uiState.update { state ->
            val pageIds = state.chapters.map { it.id }.toSet()
            state.copy(selectedChapterIds = state.selectedChapterIds + pageIds)
        }
    }

    private fun enqueueDownload() {
        val state = _uiState.value
        if (state.selectedChapterIds.isEmpty()) {
            viewModelScope.launch {
                _uiEvents.send(UserMessage.Raw(context.getString(R.string.label_search_no_chapters_selected)))
            }
            return
        }
        viewModelScope.launch {
            val baseUri = MangaDirectoryPreference.folderUriFlow(context).firstOrNull() ?: run {
                _uiEvents.send(UserMessage.Raw(context.getString(R.string.label_search_no_library_uri)))
                return@launch
            }
            // Use allSeenChapters to cover selections from any page already visited
            val ordered = state.allSeenChapters
                .filterKeys { it in state.selectedChapterIds }
                .values
                .sortedBy { it.chapter?.toDoubleOrNull() ?: Double.MAX_VALUE }
            enqueueChapters(state, baseUri, ordered)
        }
    }

    private fun downloadAll() {
        val state = _uiState.value
        val mangaId = state.manga?.mangadexId ?: return
        val language = state.selectedLanguage
        val limit = state.chaptersPerPage

        viewModelScope.launch {
            val baseUri = MangaDirectoryPreference.folderUriFlow(context).firstOrNull() ?: run {
                _uiEvents.send(UserMessage.Raw(context.getString(R.string.label_search_no_library_uri)))
                return@launch
            }
            _uiState.update { it.copy(isDownloading = true) }

            val allChapters = mutableListOf<ChapterRemoteInfoDto>()
            var page = 0
            var hasMore = true

            while (hasMore) {
                var failed = false
                searchMangaUseCase.getChaptersByLanguage(mangaId, language, page, limit).fold(
                    ifLeft = { error ->
                        _uiEvents.send(UserMessage.Raw(error.uiMessage.asString(context)))
                        _uiState.update { it.copy(isDownloading = false) }
                        failed = true
                    },
                    ifRight = { (chapters, total) ->
                        if (chapters.isEmpty()) {
                            hasMore = false
                        } else {
                            allChapters.addAll(chapters)
                            hasMore = allChapters.size < total
                            page++
                        }
                    }
                )
                if (failed) return@launch
            }

            _uiState.update { it.copy(isDownloading = false) }
            enqueueChapters(state, baseUri, allChapters)
        }
    }

    private suspend fun enqueueChapters(
        state: DownloadUiState,
        baseUri: String,
        chapters: List<ChapterRemoteInfoDto>,
    ) {
        if (chapters.isEmpty()) return
        val mangaTitle = state.manga?.title ?: return
        val chapterIds = chapters.map { it.id }.toTypedArray()
        val chapterNumbers = chapters.map { it.chapter ?: it.id }.toTypedArray()

        workManager.enqueueUniqueWork(
            "chapter_download_$mangaTitle",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                .setInputData(
                    workDataOf(
                        ChapterDownloadWorker.KEY_CHAPTER_IDS to chapterIds,
                        ChapterDownloadWorker.KEY_CHAPTER_NUMBERS to chapterNumbers,
                        ChapterDownloadWorker.KEY_MANGA_TITLE to mangaTitle,
                        ChapterDownloadWorker.KEY_FILE_EXTENSION to ArchiveFormatPattern.CBZ.extension,
                        ChapterDownloadWorker.KEY_BASE_URI to baseUri,
                        ChapterDownloadWorker.KEY_COVER_URL to state.manga?.cover?.url,
                        ChapterDownloadWorker.KEY_COVER_FILE_NAME to state.manga?.cover?.fileName,
                    )
                )
                .addTag(ChapterDownloadWorker.DOWNLOAD_TAG)
                .build()
        )

        AcerolaLogger.i(TAG, "Enqueued ${chapterIds.size} chapters for '$mangaTitle'", LogSource.VIEWMODEL)
        _uiEvents.send(UserMessage.Raw(context.getString(R.string.label_search_download_queued, chapterIds.size)))
    }

    companion object {
        private const val TAG = "DownloadViewModel"
    }
}
