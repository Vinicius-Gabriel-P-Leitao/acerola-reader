package br.acerola.manga.ui.feature.main.home.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.acerola.manga.shared.config.HomeLayoutPreferences
import br.acerola.manga.shared.config.HomeLayoutType
import br.acerola.manga.shared.dto.manga.MangaDto
import br.acerola.manga.ui.common.viewmodel.library.archive.MangaFolderViewModel
import br.acerola.manga.ui.common.viewmodel.library.metadata.MangaMetadataViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class HomeViewModelFactory(
    private val application: Application,
    private val mangaFolderViewModel: MangaFolderViewModel,
    private val mangaMetadataViewModel: MangaMetadataViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application, mangaFolderViewModel, mangaMetadataViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class HomeViewModel(
    application: Application,
    mangaFolderViewModel: MangaFolderViewModel,
    mangaMetadataViewModel: MangaMetadataViewModel,
) : AndroidViewModel(application) {
    private val context: Context get() = getApplication()

    private val _selectedHomeLayout = MutableStateFlow(value = HomeLayoutType.LIST)
    val selectedHomeLayout: StateFlow<HomeLayoutType> = _selectedHomeLayout.asStateFlow()

    val mangas: StateFlow<List<MangaDto>> = combine(
        flow = mangaFolderViewModel.folders,
        flow2 = mangaMetadataViewModel.metadata
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

    fun String.normalizeKey(): String {
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
