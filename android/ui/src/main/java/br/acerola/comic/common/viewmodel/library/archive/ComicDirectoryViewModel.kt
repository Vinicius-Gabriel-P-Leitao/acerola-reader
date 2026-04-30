package br.acerola.comic.common.viewmodel.library.archive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.acerola.comic.config.permission.FileSystemAccessManager
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.usecase.DirectoryCase
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.comic.CoverFromChapterUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.comic.UpdateComicSettingsUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
import br.acerola.comic.worker.LibrarySyncWorker
import br.acerola.comic.worker.WorkerContract
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
class ComicDirectoryViewModel
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val manager: FileSystemAccessManager,
        manageCategoriesUseCase: ManageCategoriesUseCase,
        private val coverFromChapterUseCase: CoverFromChapterUseCase,
        private val updateComicSettingsUseCase: UpdateComicSettingsUseCase,
        @param:DirectoryCase private val observeLibraryUseCase: ObserveLibraryUseCase<ComicDirectoryDto>,
        @param:DirectoryCase private val observeChaptersUseCase: ObserveChaptersUseCase<ChapterArchivePageDto>,
    ) : ViewModel() {
        private val _progress = MutableStateFlow<Int>(value = -1)
        val progress: StateFlow<Int> = _progress.asStateFlow()

        private val _isIndexing = MutableStateFlow(value = false)
        val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

        private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
        val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

        private val selectedDirectoryId = MutableStateFlow<Long?>(value = null)

        val comicDirectories: StateFlow<List<ComicDirectoryDto>> =
            observeLibraryUseCase().stateIn(
                viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            )

        val allCategories: StateFlow<List<CategoryDto>> =
            manageCategoriesUseCase
                .getAllCategories()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), emptyList())

        @OptIn(ExperimentalCoroutinesApi::class)
        val chapters: StateFlow<List<ChapterFileDto>> =
            selectedDirectoryId
                .flatMapLatest { id ->
                    id?.let {
                        observeChaptersUseCase.observeByManga(comicId = it).map { page -> page.items }
                    } ?: flowOf(value = emptyList())
                }.stateIn(
                    viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = emptyList(),
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

        fun rescanMangaByManga(comicId: Long) {
            AcerolaLogger.audit(TAG, "Requesting rescan for comic: $comicId", LogSource.VIEWMODEL)
            enqueueSync(LibrarySyncWorker.SYNC_TYPE_SPECIFIC, comicId)
        }

        fun deepScanLibrary() {
            AcerolaLogger.audit(TAG, "User requested deep library scan", LogSource.VIEWMODEL)
            enqueueSync(LibrarySyncWorker.SYNC_TYPE_REBUILD)
        }

        fun updateExternalSyncEnabled(
            comicId: Long,
            enabled: Boolean,
        ) {
            viewModelScope.launch {
                updateComicSettingsUseCase(comicId, enabled)
            }
        }

        fun extractCoverFromChapter(comicId: Long) {
            viewModelScope.launch {
                _isIndexing.value = true
                coverFromChapterUseCase(comicId).fold(
                    ifLeft = { error ->
                        _uiEvents.send(error)
                    },
                    ifRight = {
                        rescanMangaByManga(comicId)
                    },
                )
                _isIndexing.value = false
            }
        }

        private fun enqueueSync(
            type: String,
            comicId: Long? = null,
        ) {
            AcerolaLogger.d(TAG, "Enqueuing sync: $type", LogSource.VIEWMODEL)
            viewModelScope.launch {
                val uri =
                    getFolderUri() ?: if (type != LibrarySyncWorker.SYNC_TYPE_SPECIFIC) {
                        AcerolaLogger.w(TAG, "Sync aborted: base folder URI not found", LogSource.VIEWMODEL)
                        return@launch
                    } else {
                        null
                    }

                val syncRequest =
                    OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                        .setInputData(
                            workDataOf(
                                LibrarySyncWorker.KEY_SYNC_TYPE to type,
                                LibrarySyncWorker.KEY_BASE_URI to uri?.toString(),
                                LibrarySyncWorker.KEY_MANGA_ID to (comicId ?: -1L),
                            ),
                        ).addTag(WorkerContract.TAG_LIBRARY_SYNC)
                        .build()

                val workName =
                    if (comicId !=
                        null
                    ) {
                        "${WorkerContract.TAG_LIBRARY_SYNC}_$comicId"
                    } else {
                        "${WorkerContract.TAG_LIBRARY_SYNC}_unique"
                    }

                workManager.enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.KEEP,
                    syncRequest,
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
                        _progress.value = workInfo.progress.getInt(WorkerContract.KEY_PROGRESS, -1)

                        if (wasIndexing && workInfo.state.isFinished) {
                            AcerolaLogger.i(
                                TAG,
                                "Sync work finished: ${workInfo.state.name}",
                                LogSource.VIEWMODEL,
                            )

                            if (workInfo.state == WorkInfo.State.FAILED) {
                                val errorMessage = workInfo.outputData.getString(WorkerContract.KEY_ERROR)
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
