package br.acerola.manga.module.main.search

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
import br.acerola.manga.module.main.search.state.SearchAction
import br.acerola.manga.module.main.search.state.SearchUiState
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
class SearchViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val searchMangaUseCase: SearchMangaUseCase,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> _uiState.update { it.copy(query = action.query) }
            is SearchAction.Search -> search()
            is SearchAction.SelectManga -> selectManga(action.manga)
            is SearchAction.SelectLanguage -> onLanguageSelected(action.language)
            is SearchAction.ToggleChapter -> toggleChapter(action.chapterId)
            is SearchAction.ChangePage -> changePage(action.page)
            is SearchAction.SelectAll -> selectAll()
            is SearchAction.DeselectAll -> _uiState.update { it.copy(selectedChapterIds = emptySet()) }
            is SearchAction.Download -> enqueueDownload()
            is SearchAction.DownloadAll -> downloadAll()
            is SearchAction.BackToSearch -> _uiState.update {
                it.copy(selectedManga = null, chapters = emptyList(), selectedChapterIds = emptySet(), totalChapters = 0, currentPage = 0)
            }
        }
    }

    private fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isLoading = true, searchResults = emptyList(), selectedManga = null) }

        viewModelScope.launch {
            searchMangaUseCase.search(query).fold(
                ifLeft = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvents.send(UserMessage.Raw(error.uiMessage.asString(context)))
                },
                ifRight = { mangas ->
                    _uiState.update { it.copy(isLoading = false, searchResults = mangas) }
                }
            )
        }
    }

    private fun selectManga(manga: MangaRemoteInfoDto) {
        _uiState.update {
            it.copy(
                selectedManga = manga,
                isLoadingChapters = true,
                chapters = emptyList(),
                selectedChapterIds = emptySet(),
                totalChapters = 0,
                currentPage = 0,
            )
        }
        loadChapters(manga.mirrorId, _uiState.value.selectedLanguage, page = 0)
    }

    private fun onLanguageSelected(language: String) {
        val mangaId = _uiState.value.selectedManga?.mirrorId ?: return
        _uiState.update {
            it.copy(
                selectedLanguage = language,
                isLoadingChapters = true,
                chapters = emptyList(),
                selectedChapterIds = emptySet(),
                totalChapters = 0,
                currentPage = 0,
            )
        }
        loadChapters(mangaId, language, page = 0)
    }

    private fun changePage(page: Int) {
        val state = _uiState.value
        val mangaId = state.selectedManga?.mirrorId ?: return
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
                    _uiState.update {
                        it.copy(
                            isLoadingChapters = false,
                            chapters = chapters,
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

    private fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedChapterIds = state.chapters.map { it.id }.toSet())
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

            val orderedChapters = state.chapters.filter { it.id in state.selectedChapterIds }
            enqueueChapters(state, baseUri, orderedChapters)
        }
    }

    private fun downloadAll() {
        val state = _uiState.value
        val mangaId = state.selectedManga?.mirrorId ?: return
        val language = state.selectedLanguage
        val total = state.totalChapters
        val limit = state.chaptersPerPage

        viewModelScope.launch {
            val baseUri = MangaDirectoryPreference.folderUriFlow(context).firstOrNull() ?: run {
                _uiEvents.send(UserMessage.Raw(context.getString(R.string.label_search_no_library_uri)))
                return@launch
            }

            _uiState.update { it.copy(isDownloading = true) }

            val allChapters = mutableListOf<ChapterRemoteInfoDto>()
            var page = 0

            while (allChapters.size < total || (total == 0 && page == 0)) {
                val result = searchMangaUseCase.getChaptersByLanguage(mangaId, language, page, limit)
                result.fold(
                    ifLeft = { error ->
                        _uiEvents.send(UserMessage.Raw(error.uiMessage.asString(context)))
                        _uiState.update { it.copy(isDownloading = false) }
                        return@launch
                    },
                    ifRight = { (chapters, fetchedTotal) ->
                        if (chapters.isEmpty()) return@launch
                        allChapters.addAll(chapters)
                        if (allChapters.size >= fetchedTotal) return@launch
                        page++
                    }
                )
            }

            _uiState.update { it.copy(isDownloading = false) }
            enqueueChapters(state, baseUri, allChapters)
        }
    }

    private suspend fun enqueueChapters(
        state: SearchUiState,
        baseUri: String,
        chapters: List<ChapterRemoteInfoDto>
    ) {
        if (chapters.isEmpty()) return

        val mangaTitle = state.selectedManga?.title ?: return
        val fileExtension = ArchiveFormatPattern.CBZ.extension
        val chapterIds = chapters.map { it.id }.toTypedArray()
        val chapterNumbers = chapters.map { it.chapter ?: it.id }.toTypedArray()
        val coverUrl = state.selectedManga.cover?.url
        val coverFileName = state.selectedManga.cover?.fileName

        val downloadRequest = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setInputData(
                workDataOf(
                    ChapterDownloadWorker.KEY_CHAPTER_IDS to chapterIds,
                    ChapterDownloadWorker.KEY_CHAPTER_NUMBERS to chapterNumbers,
                    ChapterDownloadWorker.KEY_MANGA_TITLE to mangaTitle,
                    ChapterDownloadWorker.KEY_FILE_EXTENSION to fileExtension,
                    ChapterDownloadWorker.KEY_BASE_URI to baseUri,
                    ChapterDownloadWorker.KEY_COVER_URL to coverUrl,
                    ChapterDownloadWorker.KEY_COVER_FILE_NAME to coverFileName,
                )
            )
            .addTag(ChapterDownloadWorker.DOWNLOAD_TAG)
            .build()

        workManager.enqueueUniqueWork(
            "chapter_download_${mangaTitle}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            downloadRequest
        )

        AcerolaLogger.i(TAG, "Enqueued download of ${chapterIds.size} chapters for '$mangaTitle'", LogSource.VIEWMODEL)
        _uiEvents.send(
            UserMessage.Raw(context.getString(R.string.label_search_download_queued, chapterIds.size))
        )
    }

    companion object {
        private const val TAG = "SearchViewModel"
    }
}
