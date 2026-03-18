package br.acerola.manga.common.viewmodel.library.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.service.worker.LibrarySyncWorker
import br.acerola.manga.usecase.chapter.ObserveChaptersUseCase
import br.acerola.manga.usecase.DirectoryCase
import br.acerola.manga.util.normalizeChapter
import br.acerola.manga.config.permission.FileSystemAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChapterArchiveViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val manager: FileSystemAccessManager,
    @param:DirectoryCase private val observeChaptersUseCase: ObserveChaptersUseCase<ChapterArchivePageDto>,
) : ViewModel() {

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _progress = MutableStateFlow(value = -1)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _chapterPage = MutableStateFlow<ChapterArchivePageDto?>(value = null)

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)

    private var currentPage = 0
    private val pageSize = 20
    private var total = 0

    fun init(
        directoryId: Long,
        firstPage: ChapterArchivePageDto
    ) {
        AcerolaLogger.d(TAG, "Initializing with directoryId: $directoryId", LogSource.VIEWMODEL)
        _selectedDirectoryId.value = directoryId
        _chapterPage.value = firstPage
        currentPage = firstPage.page
        total = firstPage.total
    }

    fun loadPage(page: Int) {
        AcerolaLogger.d(TAG, "Loading local chapter page: $page", LogSource.VIEWMODEL)
        viewModelScope.launch {
            _chapterPage.value = null

            val result: ChapterArchivePageDto = observeChaptersUseCase.loadPage(
                mangaId = _selectedDirectoryId.value!!,
                pageSize = pageSize,
                total = total,
                page = page,
            )

            val sortedItems: List<ChapterFileDto> = result.items.sortedBy {
                it.chapterSort.normalizeChapter().toFloatOrNull() ?: 0f
            }

            _chapterPage.value = result.copy(items = sortedItems)
        }
    }

    fun syncChaptersByMangaDirectory(folderId: Long) {
        AcerolaLogger.audit(
            TAG, "User requested local chapter rescan", LogSource.VIEWMODEL, mapOf("folderId" to folderId.toString())
        )
        enqueueSync(LibrarySyncWorker.SYNC_TYPE_SPECIFIC, folderId)
    }

    private fun enqueueSync(
        type: String,
        mangaId: Long
    ) {
        AcerolaLogger.d(
            TAG, "Enqueuing local sync from ChapterViewModel: $type, mangaId: $mangaId", LogSource.VIEWMODEL
        )
        viewModelScope.launch {
            manager.loadFolderUri()
            val uri = manager.folderUri

            val syncRequest = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                .setInputData(
                    workDataOf(
                        LibrarySyncWorker.KEY_SYNC_TYPE to type,
                        LibrarySyncWorker.KEY_BASE_URI to uri?.toString(),
                        LibrarySyncWorker.KEY_MANGA_ID to mangaId
                    )
                )
                .addTag("library_sync")
                .build()

            workManager.enqueueUniqueWork(
                "library_sync_$mangaId",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )

            observeWorkStatus(syncRequest.id)
        }
    }

    private fun observeWorkStatus(workerId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workerId).collect { workInfo ->
                if (workInfo != null) {
                    _isIndexing.value = !workInfo.state.isFinished
                    _progress.value = workInfo.progress.getInt("progress", -1)

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

    companion object {

        private const val TAG = "ChapterArchiveViewModel"
    }
}
