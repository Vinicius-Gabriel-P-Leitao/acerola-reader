package br.acerola.manga.module.main.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import br.acerola.manga.config.preference.HomeLayoutPreference
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.usecase.DirectoryCase
import br.acerola.manga.usecase.MangadexCase
import br.acerola.manga.usecase.history.ObserveHistoryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    workManager: WorkManager,
    observeHistoryUseCase: ObserveHistoryUseCase,
    @param:ApplicationContext private val context: Context,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>,
) : ViewModel() {

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _selectedHomeLayout = MutableStateFlow(value = HomeLayoutType.LIST)
    val selectedHomeLayout: StateFlow<HomeLayoutType> = _selectedHomeLayout.asStateFlow()

    val isIndexing: StateFlow<Boolean> = workManager.getWorkInfosByTagFlow("library_sync")
        .map { workInfos ->
            workInfos.any { !it.state.isFinished }
        }.stateIn(
            viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false
        )

    val progress: StateFlow<Int> = workManager.getWorkInfosByTagFlow("library_sync")
        .map { workInfos ->
            val activeWorker = workInfos.firstOrNull { !it.state.isFinished }
            activeWorker?.progress?.getInt("progress", -1) ?: -1
        }.stateIn(
            viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = -1
        )

    val mangas: StateFlow<List<Pair<MangaDto, ReadingHistoryDto?>>> = combine(
        flow = directoryObserve(),
        flow2 = mangadexObserve(),
        flow3 = observeHistoryUseCase.invokeRecent()
    ) { mangaDirectories, remoteMangaInfo, historyList ->
        val remoteInfoMap = remoteMangaInfo.filter { it.mangaDirectoryFk != null }
            .associateBy { it.mangaDirectoryFk!! }

        val historyMap = historyList.associateBy { it.mangaDirectoryId }

        val list = mangaDirectories.map {
            val manga = MangaDto(directory = it, remoteInfo = remoteInfoMap[it.id])
            manga to historyMap[it.id]
        }

        AcerolaLogger.d(TAG, "Library loaded: ${list.size} mangas found", LogSource.VIEWMODEL)
        list
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = emptyList()
    )

    init {
        observeHomeLayout()
    }

    fun updateHomeLayout(layout: HomeLayoutType) {
        if (_selectedHomeLayout.value == layout) return
        _selectedHomeLayout.value = layout

        AcerolaLogger.audit(TAG, "User changed home layout to ${layout.name}", LogSource.VIEWMODEL)

        viewModelScope.launch {
            HomeLayoutPreference.saveLayout(context, layout)
        }
    }

    private fun observeHomeLayout() {
        viewModelScope.launch {
            HomeLayoutPreference.layoutFlow(context).collect { layout ->
                if (_selectedHomeLayout.value != layout) {
                    _selectedHomeLayout.value = layout
                }
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
