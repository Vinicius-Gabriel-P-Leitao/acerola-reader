package br.acerola.manga.common.viewmodel.library.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import arrow.core.Either
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.service.background.MetadataSyncWorker
import br.acerola.manga.usecase.di.MangadexCase
import br.acerola.manga.usecase.library.SyncLibraryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.usecase.manga.RescanMangaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaRemoteInfoViewModel @Inject constructor(
    @param:MangadexCase private val rescanManga: RescanMangaUseCase<MangaRemoteInfoDto>,
    @param:MangadexCase private val syncLibraryUseCase: SyncLibraryUseCase<MangaRemoteInfoDto>,
    @param:MangadexCase private val observeLibraryUseCase: ObserveLibraryUseCase<MangaRemoteInfoDto>,
    private val workManager: WorkManager
) : ViewModel() {

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _progress = MutableStateFlow<Int>(value = -1)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    val remoteInfo: StateFlow<List<MangaRemoteInfoDto>> = observeLibraryUseCase().stateIn(
        scope = viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    )

    fun syncLibrary() {
        viewModelScope.launch {
            _isIndexing.value = true
            syncLibraryUseCase.sync(baseUri = null).handleResult()
            _isIndexing.value = false
        }
    }

    fun rescanMangas() {
        viewModelScope.launch {
            _isIndexing.value = true
            syncLibraryUseCase.rescan(baseUri = null).handleResult()
            _isIndexing.value = false
        }
    }

    fun rescanMangaByManga(mangaId: Long) {
        viewModelScope.launch {
            _isIndexing.value = true
            rescanManga(mangaId).handleResult()
            _isIndexing.value = false
        }
    }

    fun syncFromMangadex(directoryId: Long) {
        enqueueMetadataSync(MetadataSyncWorker.SOURCE_MANGADEX, directoryId)
    }

    fun syncFromComicInfo(directoryId: Long) {
        enqueueMetadataSync(MetadataSyncWorker.SOURCE_COMICINFO, directoryId)
    }

    private fun enqueueMetadataSync(source: String, directoryId: Long) {
        viewModelScope.launch {
            val syncRequest = OneTimeWorkRequestBuilder<MetadataSyncWorker>()
                .setInputData(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to source,
                        MetadataSyncWorker.KEY_DIRECTORY_ID to directoryId
                    )
                )
                .addTag("metadata_sync")
                .build()

            workManager.enqueueUniqueWork(
                "metadata_sync_${directoryId}",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )

            observeWorkStatus(syncRequest.id)
        }
    }

    private fun observeWorkStatus(workerId: java.util.UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workerId).collect { workInfo ->
                if (workInfo != null) {
                    _isIndexing.value = !workInfo.state.isFinished
                    _progress.value = if (workInfo.state == WorkInfo.State.RUNNING) -1 else 0
                }
            }
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            _uiEvents.send(element = error)
        }
    }
}
