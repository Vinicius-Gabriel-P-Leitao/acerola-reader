package br.acerola.manga.common.viewmodel.library.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.usecase.di.MangadexCase
import br.acerola.manga.usecase.library.SyncLibraryUseCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.usecase.manga.RescanMangaUseCase
import br.acerola.manga.usecase.metadata.SyncMangaMetadataUseCase
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
    private val syncMangaMetadataUseCase: SyncMangaMetadataUseCase,
    @param:MangadexCase private val rescanManga: RescanMangaUseCase<MangaRemoteInfoDto>,
    @param:MangadexCase private val syncLibraryUseCase: SyncLibraryUseCase<MangaRemoteInfoDto>,
    @param:MangadexCase private val observeLibraryUseCase: ObserveLibraryUseCase<MangaRemoteInfoDto>
) : ViewModel() {
    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

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
        viewModelScope.launch {
            _isIndexing.value = true
            syncMangaMetadataUseCase.syncFromMangadex(directoryId).handleResult()
            _isIndexing.value = false
        }
    }

    fun syncFromComicInfo(folderId: Long) {
        viewModelScope.launch {
            _isIndexing.value = true
            syncMangaMetadataUseCase.syncFromComicInfo(folderId).handleResult()
            _isIndexing.value = false
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            _uiEvents.send(element = error)
        }
    }
}
