package br.acerola.comic.module.main.history
import br.acerola.comic.ui.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.module.main.history.state.HistoryItemState
import br.acerola.comic.usecase.DirectoryCase
import br.acerola.comic.usecase.MangadexCase
import br.acerola.comic.usecase.chapter.GetChapterCountUseCase
import br.acerola.comic.usecase.history.ObserveHistoryUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeHistoryUseCase: ObserveHistoryUseCase,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<ComicMetadataDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<ComicDirectoryDto>,
    private val manageCategoriesUseCase: ManageCategoriesUseCase,
    private val getChapterCountUseCase: GetChapterCountUseCase,
) : ViewModel() {

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: StateFlow<List<HistoryItemState>> = observeHistoryUseCase()
        .flatMapLatest { historyList ->
            combine(
                directoryObserve(),
                mangadexObserve(),
                manageCategoriesUseCase.getAllMangaCategories(),
                getChapterCountUseCase()
            ) { directories, remoteInfos, categoryMap, chapterCounts ->
                val list = historyList.mapNotNull { history ->
                    val directory = directories.find { it.id == history.mangaDirectoryId } ?: return@mapNotNull null
                    val remote = remoteInfos.find { it.mangaDirectoryFk == history.mangaDirectoryId }
                    HistoryItemState(
                        manga = ComicDto(
                            directory = directory,
                            remoteInfo = remote,
                            category = categoryMap[directory.id]
                        ),
                        history = history,
                        chapterCount = chapterCounts[directory.id] ?: 0
                    )
                }
                AcerolaLogger.d(TAG, "History items updated: ${list.size} items found", LogSource.VIEWMODEL)
                list
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    companion object {
        private const val TAG = "HistoryViewModel"
    }
}
