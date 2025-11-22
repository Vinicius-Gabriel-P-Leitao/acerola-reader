package br.acerola.manga.ui.common.viewmodel.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.ChapterPageDto
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MangaOperationViewModelFactory<T>(
    private val application: Application,
    private val libraryPort: LibraryPort<T>,
    private val folderAccessViewModel: FolderAccessViewModel,
    private val mangaOperations: LibraryPort.MangaOperations<T>,
    private val chapterOperations: LibraryPort.ChapterOperations<ChapterPageDto>,
) : ViewModelProvider.Factory {
    override fun <V : ViewModel> create(modelClass: Class<V>): V {
        if (modelClass.isAssignableFrom(MangaOperationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaOperationViewModel(
                application,
                libraryPort,
                folderAccessViewModel,
                mangaOperations,
                chapterOperations,
            ) as V
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class MangaOperationViewModel<T>(
    application: Application,
    private val libraryPort: LibraryPort<T>,
    private val folderAccessViewModel: FolderAccessViewModel,
    private val mangaOperations: LibraryPort.MangaOperations<T>,
    private val chapterOperations: LibraryPort.ChapterOperations<ChapterPageDto>,
) : AndroidViewModel(application) {
    private val _error = MutableStateFlow<Throwable?>(value = null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    val progress: StateFlow<Int> = libraryPort.progress

    private val _selectedMangaId = MutableStateFlow<Long?>(value = null)

    val mangas: StateFlow<List<T>> = mangaOperations.loadMangas().stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterFileDto>> = _selectedMangaId
        .flatMapLatest { id ->
            id?.let {
                chapterOperations.loadChapterByManga(mangaId = it)
                    .map { page -> page.items }
            } ?: flowOf(value = emptyList())
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    init {
        syncLibrary()
    }

    fun selectManga(mangaId: Long) {
        _selectedMangaId.value = mangaId
    }

    fun rescanMangas() = runLibraryTask {
        libraryPort.rescanMangas(baseUri = getFolderUri())
    }

    fun syncLibrary() = runLibraryTask {
        libraryPort.syncMangas(baseUri = getFolderUri())
    }

    fun syncChaptersByManga(mangaId: Long) = runLibraryTask {
        mangaOperations.rescanChaptersByManga(mangaId = mangaId)
    }

    fun deepScanLibrary() = runLibraryTask {
        libraryPort.deepRescanLibrary(baseUri = getFolderUri())
    }

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

    private suspend fun getFolderUri(): Uri? {
        folderAccessViewModel.loadSavedFolder()
        return folderAccessViewModel.folderUri
    }
}
