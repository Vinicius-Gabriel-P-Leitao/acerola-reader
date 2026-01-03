package br.acerola.manga.common.viewmodel.library.archive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.exception.ApplicationException
import br.acerola.manga.error.exception.GenericInternalException
import br.acerola.manga.error.handler.GlobalErrorHandler
import br.acerola.manga.repository.port.DirectoryFsOps
import br.acerola.manga.repository.port.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaDirectoryViewModel @Inject constructor(
    private val manager: FileSystemAccessManager,

    @param:DirectoryFsOps
    private val archiveSyncService: LibraryRepository<MangaDirectoryDto>,

    @param:DirectoryFsOps
    private val mangaDirectoryOperation: LibraryRepository.MangaOperations<MangaDirectoryDto>,

    @param:DirectoryFsOps
    private val chapterArchiveOperation: LibraryRepository.ChapterOperations<ChapterArchivePageDto>,
) : ViewModel() {
    val progress: StateFlow<Int> = archiveSyncService.progress

    private val _error = MutableStateFlow<Throwable?>(value = null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)
    val selectedDirectoryId: StateFlow<Long?> = _selectedDirectoryId.asStateFlow()

    val mangaDirectories: StateFlow<List<MangaDirectoryDto>> = mangaDirectoryOperation.loadMangas().stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterFileDto>> = _selectedDirectoryId.flatMapLatest { id ->
            id?.let {
                chapterArchiveOperation.loadChapterByManga(mangaId = it).map { page -> page.items }
            } ?: flowOf(value = emptyList())
        }.stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    init {
        syncLibrary()
    }

    // NOTE: Sync básico que vê só novas alterações
    fun syncLibrary() = runLibraryTask {
        val uri = getFolderUri() ?: return@runLibraryTask
        archiveSyncService.syncMangas(baseUri = uri)
    }

    // NOTE: Sync que vê só mangás novos, não faz sync de capitulos
    fun rescanMangas() = runLibraryTask {
        val uri = getFolderUri() ?: return@runLibraryTask
        archiveSyncService.rescanMangas(baseUri = uri)
    }

    // NOTE: Sync bruto, busca tudo de novo até os capitulos
    fun deepScanLibrary() = runLibraryTask {
        val uri = getFolderUri() ?: return@runLibraryTask
        archiveSyncService.deepRescanLibrary(baseUri = uri)
    }

    // TODO: A ser implementado na config de cada manga, só vai buscar os capitulos
    fun syncChaptersByMangaDirectory(folderId: Long) = runLibraryTask {
        mangaDirectoryOperation.rescanChaptersByManga(mangaId = folderId)
    }

    // TODO: Tratar melhor exceptions, de preferencia de forma personalizada e global
    private fun runLibraryTask(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isIndexing.value = true

            try {
                block()
            } catch (applicationException: ApplicationException) {
                GlobalErrorHandler.emit(applicationException)
            } catch (exception: Exception) {
                GlobalErrorHandler.emit(
                    exception = GenericInternalException(cause = exception)
                )
            } finally {
                _isIndexing.value = false
            }
        }
    }

    // TODO: Tratar melhor exceptions, de preferencia de forma personalizada e global
    private suspend fun getFolderUri(): Uri? {
        manager.loadFolderUri()
        return manager.folderUri
    }
}
