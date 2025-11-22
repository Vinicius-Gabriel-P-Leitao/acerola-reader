package br.acerola.manga.ui.common.viewmodel.library.archive

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.ChapterPageDto
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MangaFolderViewModelFactory(
    private val application: Application,
    private val libraryPort: LibraryPort<MangaFolderDto>,
    private val folderAccessViewModel: FolderAccessViewModel,
    private val mangaOperations: LibraryPort.MangaOperations<MangaFolderDto>,
    private val chapterOperations: LibraryPort.ChapterOperations<ChapterPageDto>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaFolderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MangaFolderViewModel(
                application, libraryPort, folderAccessViewModel, mangaOperations, chapterOperations,
            ) as T
        }

        // TODO: Tratar erro de forma melhor
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MangaFolderViewModel(
    application: Application,
    private val libraryPort: LibraryPort<MangaFolderDto>,
    private val folderAccessViewModel: FolderAccessViewModel,
    private val mangaOperations: LibraryPort.MangaOperations<MangaFolderDto>,
    private val chapterOperations: LibraryPort.ChapterOperations<ChapterPageDto>,
) : AndroidViewModel(application) {
    private val _error = MutableStateFlow<Throwable?>(value = null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    val progress: StateFlow<Int> = libraryPort.progress

    private val _selectedFolderId = MutableStateFlow<Long?>(value = null)

    val folders: StateFlow<List<MangaFolderDto>> = mangaOperations.loadMangas().stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterFileDto>> = _selectedFolderId
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

    fun selectFolder(folderId: Long) {
        _selectedFolderId.value = folderId
    }

    // NOTE: Sync básico que vê só novas alterações
    fun syncLibrary() = runLibraryTask {
        libraryPort.syncMangas(baseUri = getFolderUri())
    }

    // NOTE: Sync que vê só mangás novos, não faz sync de capitulos
    fun rescanMangas() = runLibraryTask {
        libraryPort.rescanMangas(baseUri = getFolderUri())
    }

    // NOTE: Sync bruto, busca tudo de novo até os capitulos
    fun deepScanLibrary() = runLibraryTask {
        libraryPort.deepRescanLibrary(baseUri = getFolderUri())
    }

    // TODO: A ser implementado na config de cada manga, só vai buscar os capitulos
    fun syncChaptersByFolder(folderId: Long) = runLibraryTask {
        mangaOperations.rescanChaptersByManga(mangaId = folderId)
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
