package br.acerola.manga.ui.feature.main.home.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.config.preference.HomeLayoutPreferences
import br.acerola.manga.shared.config.preference.HomeLayoutType
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.shared.dto.manga.MangaDto
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
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
    private val folderPort: LibraryPort<MangaFolderDto>,
    private val folderOps: LibraryPort.MangaOperations<MangaFolderDto>,
    private val metadataPort: LibraryPort<MangaMetadataDto>,
    private val metadataOps: LibraryPort.MangaOperations<MangaMetadataDto>,
) : ViewModel() {

    private val _selectedHomeLayout = MutableStateFlow(HomeLayoutType.LIST)
    val selectedHomeLayout: StateFlow<HomeLayoutType> = _selectedHomeLayout.asStateFlow()

    val isIndexing: StateFlow<Boolean> = combine(
        flow = folderPort.isIndexing,
        flow2 = metadataPort.isIndexing
    ) { folderIndexing, metadataIndexing ->
        folderIndexing || metadataIndexing
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val progress: StateFlow<Int> = combine(
        flow = folderPort.isIndexing,
        flow2 = folderPort.progress,
        flow3 = metadataPort.isIndexing,
        flow4 = metadataPort.progress
    ) { folderBusy, folderProg, metadataBusy, metadataProg ->
        when {
            folderBusy && folderProg != -1 -> folderProg
            metadataBusy && metadataProg != -1 -> metadataProg
            else -> -1
        }
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = -1
    )

    val mangas: StateFlow<List<MangaDto>> = combine(
        flow = folderOps.loadMangas(),
        flow2 = metadataOps.loadMangas()
    ) { folders, metadata ->
        val metadataMap = metadata.associateBy { it.title.normalizeKey() }
        folders.map { folder ->
            MangaDto(folder = folder, metadata = metadataMap[folder.name.normalizeKey()])
        }
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    init {
        observeHomeLayout()
    }

    private fun String.normalizeKey(): String {
        return this.filter { it.isLetterOrDigit() }.lowercase()
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
}