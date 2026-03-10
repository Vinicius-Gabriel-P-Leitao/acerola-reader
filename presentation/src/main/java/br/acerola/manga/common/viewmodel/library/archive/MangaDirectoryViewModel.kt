package br.acerola.manga.common.viewmodel.library.archive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import arrow.core.Either
import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.service.background.LibrarySyncWorker
import br.acerola.manga.usecase.chapter.GetChaptersUseCase
import br.acerola.manga.usecase.di.DirectoryCase
import br.acerola.manga.usecase.library.SyncLibraryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.usecase.manga.RescanMangaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MangaDirectoryViewModel @Inject constructor(
    private val manager: FileSystemAccessManager,
    @param:DirectoryCase private val rescanManga: RescanMangaUseCase<MangaDirectoryDto>,
    @param:DirectoryCase private val getChaptersUseCase: GetChaptersUseCase<ChapterArchivePageDto>,
    @param:DirectoryCase private val observeLibraryUseCase: ObserveLibraryUseCase<MangaDirectoryDto>,
    private val workManager: WorkManager
) : ViewModel() {

    private val _progress = MutableStateFlow<Int>(value = -1)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)
    val selectedDirectoryId: StateFlow<Long?> = _selectedDirectoryId.asStateFlow()

    val mangaDirectories: StateFlow<List<MangaDirectoryDto>> = observeLibraryUseCase().stateIn(
        viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterFileDto>> = _selectedDirectoryId.flatMapLatest { id ->
        id?.let {
            getChaptersUseCase.observeByManga(mangaId = it).map { page -> page.items }
        } ?: flowOf(value = emptyList())
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList()
    )

    init {
        syncLibrary()
    }

    fun syncLibrary() {
        enqueueSync(LibrarySyncWorker.SYNC_TYPE_INCREMENTAL)
    }

    fun rescanMangas() {
        enqueueSync(LibrarySyncWorker.SYNC_TYPE_REFRESH)
    }

    fun rescanMangaByManga(mangaId: Long) {
        viewModelScope.launch {
            _isIndexing.value = true
            rescanManga(mangaId).handleResult()
            _isIndexing.value = false
        }
    }

    fun deepScanLibrary() {
        enqueueSync(LibrarySyncWorker.SYNC_TYPE_REBUILD)
    }

    private fun enqueueSync(type: String) {
        viewModelScope.launch {
            val uri = getFolderUri() ?: return@launch

            val syncRequest = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                .setInputData(
                    workDataOf(
                        LibrarySyncWorker.KEY_SYNC_TYPE to type,
                        LibrarySyncWorker.KEY_BASE_URI to uri.toString()
                    )
                )
                .addTag("library_sync")
                .build()

            workManager.enqueueUniqueWork(
                "library_sync_unique",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )

            observeWorkProgress(syncRequest.id)
        }
    }

    private fun observeWorkProgress(workerId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workerId).collect { workInfo ->
                if (workInfo != null) {
                    _isIndexing.value = !workInfo.state.isFinished
                    _progress.value = workInfo.progress.getInt("progress", -1)
                }
            }
        }
    }

    private suspend fun getFolderUri(): Uri? {
        manager.loadFolderUri()
        return manager.folderUri
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            _uiEvents.send(element = error)
        }
    }
}