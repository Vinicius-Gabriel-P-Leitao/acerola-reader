package br.acerola.manga.ui.feature.main.home.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.service.archive.ArchiveMangaService
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

class MangaLibraryViewModel(
    application: Application,
    private val archiveService: ArchiveMangaService,
    private val folderAccessViewModel: FolderAccessViewModel,
) : AndroidViewModel(application) {
    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(value = null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    val progress: StateFlow<Int> = archiveService.progress

    private val _selectedFolderId = MutableStateFlow<Long?>(value = null)
    fun selectFolder(folderId: Long) {
        _selectedFolderId.value = folderId
    }

    val folders: StateFlow<List<MangaFolderDto>> = archiveService.getAllFolders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterFileDto>> = _selectedFolderId.flatMapLatest { folderId ->
        folderId?.let { id ->
            archiveService.getChaptersByFolder(folderId = id)
        } ?: flowOf(value = emptyList())
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList()
    )

    private fun runIndexing(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isIndexing.value = true
            _error.value = null
            val startTime = System.currentTimeMillis()
            try {
                block()
            } catch (e: Exception) {
                _error.value = e
            } finally {
                val duration = System.currentTimeMillis() - startTime
                val minDisplayTime = 500L // 0.5 seconds
                if (duration < minDisplayTime) {
                    delay(minDisplayTime - duration)
                }
                _isIndexing.value = false
            }
        }
    }

    private suspend fun getFolderUri(): Uri {
        folderAccessViewModel.loadSavedFolder()
        return folderAccessViewModel.folderUri ?: throw IllegalStateException("Nenhuma pasta salva encontrada.")
    }

    fun indexLibraryFromSavedFolder() {
        runIndexing {
            archiveService.indexLibrary(baseUri = getFolderUri())
        }
    }

    fun quickIndexLibraryFromSavedFolder() {
        runIndexing {
            archiveService.quickIndexLibrary(baseUri = getFolderUri())
        }
    }

    fun indexLibrary(uri: Uri) {
        runIndexing {
            archiveService.indexLibrary(baseUri = uri)
        }
    }

    fun quickIndexLibrary(uri: Uri) {
        runIndexing {
            archiveService.quickIndexLibrary(baseUri = uri)
        }
    }
}