package br.acerola.manga.module.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.preference.HomeLayoutPreference
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.usecase.di.DirectoryCase
import br.acerola.manga.usecase.di.MangadexCase
import br.acerola.manga.usecase.library.SyncLibraryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
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
    @param:DirectoryCase private val directorySync: SyncLibraryUseCase<MangaDirectoryDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>,
    @param:MangadexCase private val mangadexSync: SyncLibraryUseCase<MangaRemoteInfoDto>,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>,
) : ViewModel() {

    private val _selectedHomeLayout = MutableStateFlow(value = HomeLayoutType.LIST)
    val selectedHomeLayout: StateFlow<HomeLayoutType> = _selectedHomeLayout.asStateFlow()

    val isIndexing: StateFlow<Boolean> = combine(
        flow = directorySync.isIndexing, flow2 = mangadexSync.isIndexing
    ) { directoryIndexing, remoteInfoIndexing ->
        directoryIndexing || remoteInfoIndexing
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = false
    )

    val progress: StateFlow<Int> = combine(
        flow = directorySync.isIndexing,
        flow2 = directorySync.progress,
        flow3 = mangadexSync.isIndexing,
        flow4 = mangadexSync.progress
    ) { directoryBusy, directoryProg, remoteInfoBusy, remoteInfoProg ->
        when {
            directoryBusy && directoryProg != -1 -> directoryProg
            remoteInfoBusy && remoteInfoProg != -1 -> remoteInfoProg
            else -> -1
        }
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = -1
    )

    val mangas: StateFlow<List<MangaDto>> = combine(
        flow = directoryObserve(), flow2 = mangadexObserve()
    ) { mangaDirectories, remoteMangaInfo ->
        // NOTE: Agora associa pelo FK direto do banco, muito mais seguro que por título
        val remoteInfoMap = remoteMangaInfo.filter { it.mangaDirectoryFk != null }
            .associateBy { it.mangaDirectoryFk!! }

        mangaDirectories.map {
            MangaDto(directory = it, remoteInfo = remoteInfoMap[it.id])
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