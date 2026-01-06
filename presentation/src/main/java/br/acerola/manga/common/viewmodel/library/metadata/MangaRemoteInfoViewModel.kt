package br.acerola.manga.common.viewmodel.library.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.repository.di.MangadexFsOps
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
    @param:MangadexFsOps
    private val mangadexSyncRepository: LibraryRepository<MangaRemoteInfoDto>,

    @param:MangadexFsOps
    private val mangadexChapterRepository: LibraryRepository.MangaOperations<MangaRemoteInfoDto>,
) : ViewModel() {
    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    val progress: StateFlow<Int> = mangadexSyncRepository.progress

    val remoteInfo: StateFlow<List<MangaRemoteInfoDto>> = mangadexChapterRepository.loadMangas()
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        )

    fun syncLibrary() {
        viewModelScope.launch {
            _isIndexing.value = true
            mangadexSyncRepository.syncMangas(baseUri = null).handleResult()
            _isIndexing.value = false
        }
    }

    fun rescanMangas() {
        viewModelScope.launch {
            _isIndexing.value = true
            mangadexSyncRepository.rescanMangas(baseUri = null).handleResult()
            _isIndexing.value = false
        }
    }

    fun syncChaptersByMangaRemoteInfo(mangaId: Long) {
        viewModelScope.launch {
            _isIndexing.value = true
            mangadexChapterRepository.rescanChaptersByManga(mangaId = mangaId).handleResult()
            _isIndexing.value = false
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            _uiEvents.send(element = error)
        }
    }
}
