package br.acerola.manga.module.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.preference.HomeLayoutPreferences
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.repository.port.DirectoryFsOps
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.repository.port.MangadexFsOps
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,

    @param:DirectoryFsOps
    private val archiveSyncService: LibraryRepository<MangaDirectoryDto>,

    @param:DirectoryFsOps
    private val mangaDirectoryOperation: LibraryRepository.MangaOperations<MangaDirectoryDto>,

    @param:MangadexFsOps
    private val mangadexSyncService: LibraryRepository<MangaRemoteInfoDto>,

    @param:MangadexFsOps
    private val mangadexRemoteInfoOperation: LibraryRepository.MangaOperations<MangaRemoteInfoDto>,
) : ViewModel() {

    private val _selectedHomeLayout = MutableStateFlow(value = HomeLayoutType.LIST)
    val selectedHomeLayout: StateFlow<HomeLayoutType> = _selectedHomeLayout.asStateFlow()

    val isIndexing: StateFlow<Boolean> = combine(
        flow = archiveSyncService.isIndexing, flow2 = mangadexSyncService.isIndexing
    ) { directoryIndexing, remoteInfoIndexing ->
        directoryIndexing || remoteInfoIndexing
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val progress: StateFlow<Int> = combine(
        flow = archiveSyncService.isIndexing,
        flow2 = archiveSyncService.progress,
        flow3 = mangadexSyncService.isIndexing,
        flow4 = mangadexSyncService.progress
    ) { directoryBusy, directoryProg, remoteInfoBusy, remoteInfoProg ->
        when {
            directoryBusy && directoryProg != -1 -> directoryProg
            remoteInfoBusy && remoteInfoProg != -1 -> remoteInfoProg
            else -> -1
        }
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = -1
    )

    val mangas: StateFlow<List<MangaDto>> = combine(
        flow = mangaDirectoryOperation.loadMangas(), flow2 = mangadexRemoteInfoOperation.loadMangas()
    ) { mangaDirectories, remoteMangaInfo ->
        val remoteInfoMap = remoteMangaInfo.associateBy { it.title.normalizeKey() }

        mangaDirectories.map {
            MangaDto(directory = it, remoteInfo = remoteInfoMap[it.name.normalizeKey()])
        }
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    init {
        observeHomeLayout()
    }

    fun updateHomeLayout(layout: HomeLayoutType) {
        if (_selectedHomeLayout.value == layout) return
        _selectedHomeLayout.value = layout
        viewModelScope.launch {
            HomeLayoutPreferences.saveLayout(context, layout)
        }
    }

    private fun observeHomeLayout() {
        viewModelScope.launch {
            HomeLayoutPreferences.layoutFlow(context).collect { layout ->
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