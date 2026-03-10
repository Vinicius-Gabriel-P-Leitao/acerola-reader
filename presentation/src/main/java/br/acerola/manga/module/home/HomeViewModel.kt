package br.acerola.manga.module.home

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
import br.acerola.manga.repository.port.HistoryManagementRepository
import br.acerola.manga.usecase.di.DirectoryCase
import br.acerola.manga.usecase.di.MangadexCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>,
    private val historyRepository: HistoryManagementRepository,
    private val workManager: WorkManager
) : ViewModel() {

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
        flow3 = historyRepository.getAllRecentHistory()
    ) { mangaDirectories, remoteMangaInfo, historyList ->
        val remoteInfoMap = remoteMangaInfo.filter { it.mangaDirectoryFk != null }
            .associateBy { it.mangaDirectoryFk!! }

        val historyMap = historyList.associateBy { it.mangaDirectoryId }

        mangaDirectories.map {
            val manga = MangaDto(directory = it, remoteInfo = remoteInfoMap[it.id])
            manga to historyMap[it.id]
        }
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = emptyList()
    )

    init {
        observeHomeLayout()
    }

    fun updateHomeLayout(layout: HomeLayoutType) {
        if (_selectedHomeLayout.value == layout) return
        _selectedHomeLayout.value = layout

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

    private fun String.normalizeKey(): String {
        return this.filter { it.isLetterOrDigit() }.lowercase()
    }
}