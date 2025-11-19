package br.acerola.manga.ui.common.viewmodel.library

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.config.HomeLayoutPreferences
import br.acerola.manga.shared.config.HomeLayoutType
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MangaLibraryViewModelFactory(
    private val application: Application,
    private val libraryPort: LibraryPort,
    private val mangaOperations: LibraryPort.MangaOperations,
    private val chapterOperations: LibraryPort.ChapterOperations,
    private val folderAccessViewModel: FolderAccessViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaLibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MangaLibraryViewModel(
                application, libraryPort, mangaOperations, chapterOperations, folderAccessViewModel
            ) as T
        }

        // TODO: Tratar erro de forma melhor
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MangaLibraryViewModel(
    application: Application,
    private val libraryPort: LibraryPort,
    private val mangaOperations: LibraryPort.MangaOperations,
    private val chapterOperations: LibraryPort.ChapterOperations,
    private val folderAccessViewModel: FolderAccessViewModel,
) : AndroidViewModel(application) {
    private val context: Context get() = getApplication()

    private val _error = MutableStateFlow<Throwable?>(value = null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _selectedHomeLayout = MutableStateFlow(value = HomeLayoutType.LIST)
    val selectedHomeLayout: StateFlow<HomeLayoutType> = _selectedHomeLayout.asStateFlow()

    val progress: StateFlow<Int> = libraryPort.progress

    private val _selectedFolderId = MutableStateFlow<Long?>(value = null)

    val folders: StateFlow<List<MangaFolderDto>> = mangaOperations.loadMangas().stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterFileDto>> = _selectedFolderId.flatMapLatest { id ->
        id?.let { chapterOperations.loadChapterByManga(mangaId = it) } ?: flowOf(value = emptyList())
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = emptyList()
    )

    init {
        observeHomeLayout()
        syncLibrary()
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

    fun updateHomeLayout(layout: HomeLayoutType) {
        if (_selectedHomeLayout.value == layout) return
        _selectedHomeLayout.value = layout
        viewModelScope.launch {
            HomeLayoutPreferences.saveLayout(context, layout)
        }
    }

    fun selectFolder(folderId: Long) {
        _selectedFolderId.value = folderId
    }

    fun rescanMangas() = runLibraryTask {
        libraryPort.rescanMangas(baseUri = getFolderUri())
    }

    fun syncLibrary() = runLibraryTask {
        libraryPort.syncMangas(baseUri = getFolderUri())
    }

    fun syncChaptersByFolder(folderId: Long) = runLibraryTask {
        mangaOperations.rescanChaptersByManga(mangaId = folderId)
    }

    fun deepScanLibrary() = runLibraryTask {
        libraryPort.deepRescanLibrary(baseUri = getFolderUri())
    }

    // TODO: Tratar melhor exceptions, de preferencia de forma personalizada e global
    private fun runLibraryTask(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isIndexing.value = true
            _error.value = null
            val start = System.currentTimeMillis()
            try {
                block()
            } catch (e: Exception) {
                _error.value = e
            } finally {
                val elapsed = System.currentTimeMillis() - start
                val minTime = 500L
                if (elapsed < minTime) delay(timeMillis = minTime - elapsed)
                _isIndexing.value = false
            }
        }
    }

    // TODO: Tratar melhor exceptions, de preferencia de forma personalizada e global
    private suspend fun getFolderUri(): Uri {
        folderAccessViewModel.loadSavedFolder()
        return folderAccessViewModel.folderUri ?: throw IllegalStateException("Nenhuma pasta salva encontrada.")
    }
}
