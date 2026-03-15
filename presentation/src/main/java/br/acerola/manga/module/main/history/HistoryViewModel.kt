package br.acerola.manga.module.main.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.module.main.history.state.HistoryUiState
import br.acerola.manga.repository.port.HistoryManagementRepository
import br.acerola.manga.usecase.di.DirectoryCase
import br.acerola.manga.usecase.di.MangadexCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryManagementRepository,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: StateFlow<List<HistoryUiState>> = historyRepository.getAllRecentHistoryWithChapter()
        .flatMapLatest { historyList ->
            combine(
                directoryObserve(),
                mangadexObserve()
            ) { directories, remoteInfos ->
                val list = historyList.mapNotNull { history ->
                    val directory = directories.find { it.id == history.mangaDirectoryId } ?: return@mapNotNull null
                    val remote = remoteInfos.find { it.mangaDirectoryFk == history.mangaDirectoryId }
                    HistoryUiState(
                        manga = MangaDto(directory = directory, remoteInfo = remote),
                        history = history
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
