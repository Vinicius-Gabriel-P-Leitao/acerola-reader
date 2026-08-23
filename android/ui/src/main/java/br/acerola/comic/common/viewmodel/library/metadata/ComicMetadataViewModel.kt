package br.acerola.comic.common.viewmodel.library.metadata
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.usecase.MangadexCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
import br.acerola.comic.worker.contract.WorkerContract
import br.acerola.comic.worker.sync.MetadataSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ComicMetadataViewModel
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val manageCategoriesUseCase: ManageCategoriesUseCase,
        @param:MangadexCase private val observeLibraryUseCase: ObserveLibraryUseCase<ComicMetadataDto>,
    ) : ViewModel() {
        private val _workerIndexing = MutableStateFlow(false)

        val activeSyncSource: StateFlow<String?> =
            workManager
                .getWorkInfosByTagFlow("metadata_sync")
                .map { list ->
                    val running = list.firstOrNull { !it.state.isFinished } ?: return@map null
                    when {
                        running.tags.contains("source_${MetadataSyncWorker.SOURCE_MANGADEX}") -> MetadataSyncWorker.SOURCE_MANGADEX
                        running.tags.contains("source_${MetadataSyncWorker.SOURCE_ANILIST}") -> MetadataSyncWorker.SOURCE_ANILIST
                        running.tags.contains("source_${MetadataSyncWorker.SOURCE_COMICINFO}") -> MetadataSyncWorker.SOURCE_COMICINFO
                        else -> null
                    }
                }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

        val activeSyncType: StateFlow<String?> =
            workManager
                .getWorkInfosByTagFlow("metadata_sync")
                .map { list ->
                    val running = list.firstOrNull { !it.state.isFinished } ?: return@map null
                    when {
                        running.tags.contains("type_${MetadataSyncWorker.SYNC_TYPE_RESCAN}") -> MetadataSyncWorker.SYNC_TYPE_RESCAN
                        running.tags.contains("type_${MetadataSyncWorker.SYNC_TYPE_SYNC}") -> MetadataSyncWorker.SYNC_TYPE_SYNC
                        else -> null
                    }
                }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

        val isIndexing: StateFlow<Boolean> =
            combine(
                observeLibraryUseCase.isIndexing,
                _workerIndexing,
            ) { repoIndexing, workerBusy ->
                repoIndexing || workerBusy
            }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

        val progress: StateFlow<Int> =
            observeLibraryUseCase.progress
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

        private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
        val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

        val remoteInfo: StateFlow<List<ComicMetadataDto>> =
            observeLibraryUseCase().stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            )

        val allCategories: StateFlow<List<CategoryDto>> =
            manageCategoriesUseCase.getAllCategories().stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            )

        fun createCategory(
            name: String,
            color: Int,
        ) {
            viewModelScope.launch {
                manageCategoriesUseCase.createCategory(name, color)
            }
        }

        fun deleteCategory(id: Long) {
            viewModelScope.launch {
                manageCategoriesUseCase.deleteCategory(id)
            }
        }

        fun updateMangaCategory(
            directoryId: Long,
            categoryId: Long?,
        ) {
            viewModelScope.launch {
                manageCategoriesUseCase.updateComicCategory(directoryId, categoryId)
            }
        }

        fun syncLibrary() {
            AcerolaLogger.audit(TAG, "User requested library metadata sync", LogSource.VIEWMODEL)
            enqueueMetadataSync(MetadataSyncWorker.SOURCE_MANGADEX, -1L, MetadataSyncWorker.SYNC_TYPE_SYNC)
        }

        fun rescanMangas() {
            AcerolaLogger.audit(TAG, "User requested library metadata rescan", LogSource.VIEWMODEL)
            enqueueMetadataSync(MetadataSyncWorker.SOURCE_MANGADEX, -1L, MetadataSyncWorker.SYNC_TYPE_RESCAN)
        }

        fun rescanAnilistMangas() {
            AcerolaLogger.audit(TAG, "User requested library AniList metadata rescan", LogSource.VIEWMODEL)
            enqueueMetadataSync(MetadataSyncWorker.SOURCE_ANILIST, -1L, MetadataSyncWorker.SYNC_TYPE_RESCAN)
        }

        fun rescanMangaByManga(comicId: Long) {
            AcerolaLogger.audit(TAG, "User requested metadata rescan for comic: $comicId", LogSource.VIEWMODEL)
            enqueueMetadataSync(MetadataSyncWorker.SOURCE_MANGADEX, comicId, MetadataSyncWorker.SYNC_TYPE_RESCAN)
        }

        fun syncFromMangadex(directoryId: Long) {
            AcerolaLogger.audit(
                TAG,
                "User requested metadata sync from MangaDex",
                LogSource.VIEWMODEL,
                mapOf("directoryId" to directoryId.toString()),
            )
            enqueueMetadataSync(MetadataSyncWorker.SOURCE_MANGADEX, directoryId, MetadataSyncWorker.SYNC_TYPE_SYNC)
        }

        fun syncFromComicInfo(directoryId: Long) {
            AcerolaLogger.audit(
                TAG,
                "User requested metadata sync from ComicInfo.xml",
                LogSource.VIEWMODEL,
                mapOf("directoryId" to directoryId.toString()),
            )
            enqueueMetadataSync(MetadataSyncWorker.SOURCE_COMICINFO, directoryId, MetadataSyncWorker.SYNC_TYPE_SYNC)
        }

        fun syncFromAnilist(directoryId: Long) {
            AcerolaLogger.audit(
                TAG,
                "User requested metadata sync from AniList",
                LogSource.VIEWMODEL,
                mapOf("directoryId" to directoryId.toString()),
            )
            enqueueMetadataSync(MetadataSyncWorker.SOURCE_ANILIST, directoryId, MetadataSyncWorker.SYNC_TYPE_SYNC)
        }

        private fun enqueueMetadataSync(
            source: String,
            directoryId: Long,
            type: String = MetadataSyncWorker.SYNC_TYPE_SYNC,
        ) {
            AcerolaLogger.d(
                TAG,
                "Enqueuing metadata sync worker: source=$source, directoryId=$directoryId, type=$type",
                LogSource.VIEWMODEL,
            )
            viewModelScope.launch {
                val syncRequest =
                    OneTimeWorkRequestBuilder<MetadataSyncWorker>()
                        .setInputData(
                            workDataOf(
                                MetadataSyncWorker.KEY_SYNC_TYPE to type,
                                MetadataSyncWorker.KEY_SYNC_SOURCE to source,
                                MetadataSyncWorker.KEY_DIRECTORY_ID to directoryId,
                            ),
                        ).addTag("metadata_sync")
                        .addTag("source_$source")
                        .addTag("type_$type")
                        .build()

                val workName = if (directoryId == -1L) "metadata_sync_library_$source" else "metadata_sync_${source}_$directoryId"

                _workerIndexing.value = true

                workManager.enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.REPLACE,
                    syncRequest,
                )

                observeWorkProgress(syncRequest.id)
            }
        }

        private fun observeWorkProgress(workerId: UUID) {
            viewModelScope.launch {
                workManager.getWorkInfoByIdFlow(workerId).collect { workInfo ->
                    if (workInfo != null) {
                        _workerIndexing.value = !workInfo.state.isFinished

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

        companion object {
            private const val TAG = "MangaRemoteInfoViewModel"
        }
    }
