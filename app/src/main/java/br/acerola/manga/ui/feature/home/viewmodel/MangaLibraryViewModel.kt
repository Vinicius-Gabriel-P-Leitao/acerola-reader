package br.acerola.manga.ui.feature.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.service.archive.ArchiveMangaService
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    val folders: StateFlow<List<MangaFolderDto>> = archiveService.getAllFoldersDto().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterFileDto>> = _selectedFolderId.flatMapLatest { folderId ->
        folderId?.let { id ->
            archiveService.getChaptersByFolderDto(folderId = id)
        } ?: flowOf(value = emptyList())
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList()
    )

    // TODO: Adicionar validaçõa de erro personalizada.
    fun indexLibraryFromSavedFolder() {
        viewModelScope.launch {
            _isIndexing.value = true
            _error.value = null

            try {
                folderAccessViewModel.loadSavedFolder()
                folderAccessViewModel.folderUri?.let { uri ->
                    archiveService.indexLibrary(baseUri = uri)
                } ?: run {
                    _error.value = IllegalStateException("Nenhuma pasta salva encontrada.")
                }
            } catch (exception: Exception) {
                _error.value = exception
            } finally {
                _isIndexing.value = false
            }
        }
    }
}