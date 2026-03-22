package br.acerola.manga.module.main.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.error.UserMessage
import br.acerola.manga.module.main.search.state.SearchAction
import br.acerola.manga.module.main.search.state.SearchUiState
import br.acerola.manga.core.usecase.search.SearchMangaUseCase
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

import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.acerola.manga.core.worker.ChapterDownloadWorker
import br.acerola.manga.module.main.search.state.DownloadProgress
import kotlinx.coroutines.flow.collectLatest

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

    init {
        observeDownloadQueue()
    }

    private fun observeDownloadQueue() {
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow(ChapterDownloadWorker.DOWNLOAD_TAG).collectLatest { workInfos ->
                val queue = workInfos
                    .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    .map { info ->
                        val mangaTitle = info.tags.firstOrNull { it.startsWith("chapter_download_") && it != ChapterDownloadWorker.DOWNLOAD_TAG }
                            ?.removePrefix("chapter_download_")
                            ?: info.progress.getString(ChapterDownloadWorker.KEY_MANGA_TITLE)
                            ?: "Manga"

                        DownloadProgress(
                            mangaTitle = mangaTitle,
                            progress = info.progress.getInt("progress", 0),
                            currentChapterId = info.progress.getString("currentChapterId"),
                            currentChapterFileName = info.progress.getString("currentChapterFileName"),
                            totalChapters = info.progress.getInt("totalChapters", 0),
                            isRunning = info.state == WorkInfo.State.RUNNING
                        )
                    }
                _uiState.update { it.copy(downloadQueue = queue) }
            }
        }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> _uiState.update { it.copy(query = action.query) }
            is SearchAction.Search -> search()
        }
    }

    private fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isLoading = true, searchResults = emptyList()) }

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
}
