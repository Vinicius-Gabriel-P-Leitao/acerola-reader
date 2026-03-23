package br.acerola.manga.common.viewmodel.library.archive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.core.usecase.DirectoryCase
import br.acerola.manga.core.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.core.usecase.manga.ExtractCoverFromChapterUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.core.worker.LibrarySyncWorker
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
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
    private val workManager: WorkManager,
    private val manager: FileSystemAccessManager,
    private val extractCoverFromChapterUseCase: ExtractCoverFromChapterUseCase,
    @param:DirectoryCase private val observeLibraryUseCase: ObserveLibraryUseCase<MangaDirectoryDto>,
    @param:DirectoryCase private val observeChaptersUseCase: ObserveChaptersUseCase<ChapterArchivePageDto>,
) : ViewModel() {

    private val _progress = MutableStateFlow<Int>(value = -1)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)

    val mangaDirectories: StateFlow<List<MangaDirectoryDto>> = observeLibraryUseCase().stateIn(
        viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterFileDto>> = _selectedDirectoryId.flatMapLatest { id ->
        id?.let {
            observeChaptersUseCase.observeByManga(mangaId = it).map { page -> page.items }
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
        AcerolaLogger.audit(TAG, "Requesting rescan for manga: $mangaId", LogSource.VIEWMODEL)
        enqueueSync(LibrarySyncWorker.SYNC_TYPE_SPECIFIC, mangaId)
    }

    fun deepScanLibrary() {
        AcerolaLogger.audit(TAG, "User requested deep library scan", LogSource.VIEWMODEL)
        enqueueSync(LibrarySyncWorker.SYNC_TYPE_REBUILD)
    }

    fun updateExternalSyncEnabled(mangaId: Long, enabled: Boolean) {
        viewModelScope.launch {
            observeLibraryUseCase.updateMangaSettings(mangaId, enabled)
        }
    }

    fun extractCoverFromChapter(mangaId: Long) {
        viewModelScope.launch {
            _isIndexing.value = true
            extractCoverFromChapterUseCase(mangaId).fold(
                ifLeft = { error ->
                    _uiEvents.send(error)
                },
                ifRight = {
                    rescanMangaByManga(mangaId)
                }
            )
            _isIndexing.value = false
        }
    }

    private fun enqueueSync(
        type: String,
        mangaId: Long? = null
    ) {
        AcerolaLogger.d(TAG, "Enqueuing sync: $type", LogSource.VIEWMODEL)
        viewModelScope.launch {
            val uri = getFolderUri() ?: if (type != LibrarySyncWorker.SYNC_TYPE_SPECIFIC) {
                AcerolaLogger.w(TAG, "Sync aborted: base folder URI not found", LogSource.VIEWMODEL)
                return@launch
            } else null

            val syncRequest = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                .setInputData(
                    workDataOf(
                        LibrarySyncWorker.KEY_SYNC_TYPE to type,
                        LibrarySyncWorker.KEY_BASE_URI to uri?.toString(),
                        LibrarySyncWorker.KEY_MANGA_ID to (mangaId ?: -1L)
                    )
                )
                .addTag("library_sync")
                .build()

            val workName = if (mangaId != null) "library_sync_$mangaId" else "library_sync_unique"

            workManager.enqueueUniqueWork(
                workName,
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
                    val wasIndexing = _isIndexing.value
                    _isIndexing.value = !workInfo.state.isFinished
                    _progress.value = workInfo.progress.getInt("progress", -1)

                    if (wasIndexing && workInfo.state.isFinished) {
                        AcerolaLogger.i(
                            TAG, "Sync work finished: ${workInfo.state.name}", LogSource.VIEWMODEL
                        )

                        if (workInfo.state == WorkInfo.State.FAILED) {
                            val errorMessage = workInfo.outputData.getString("error")
                            if (errorMessage != null) {
                                _uiEvents.send(UserMessage.Raw(errorMessage))
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getFolderUri(): Uri? {
        manager.loadFolderUri()
        return manager.folderUri
    }

    companion object {

        private const val TAG = "MangaDirectoryViewModel"
    }
}
