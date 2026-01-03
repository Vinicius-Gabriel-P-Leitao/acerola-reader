package br.acerola.manga.common.viewmodel.library.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.exception.ApplicationException
import br.acerola.manga.error.exception.GenericInternalException
import br.acerola.manga.error.handler.GlobalErrorHandler
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.repository.port.MangadexFsOps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaRemoteInfoViewModel @Inject constructor(
    @param:MangadexFsOps
    private val mangadexSyncService: LibraryRepository<MangaRemoteInfoDto>,

    @param:MangadexFsOps
    private val mangadexRemoteInfoOperation: LibraryRepository.MangaOperations<MangaRemoteInfoDto>,
) : ViewModel() {
    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    val progress: StateFlow<Int> = mangadexSyncService.progress

    val remoteInfo: StateFlow<List<MangaRemoteInfoDto>> = mangadexRemoteInfoOperation.loadMangas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    fun syncLibrary() = runLibraryTask {
        mangadexSyncService.syncMangas(baseUri = null)
    }

    fun rescanMangas() = runLibraryTask {
        mangadexSyncService.rescanMangas(baseUri = null)
    }

    /**
     *  Não implementádo no service o rescanMangas é feito de rebind no deepRescanLibrary, então dão ná mesma.
     *
     *  fun deepScanLibrary() = runLibraryTask {
     *      mangadexSyncService.deepRescanLibrary(baseUri = null)
     *  }
     */

    // TODO: A ser implementado na config de cada manga, só vai buscar os capitulos
    fun syncChaptersByMangaRemoteInfo(mangaId: Long) = runLibraryTask {
        mangadexRemoteInfoOperation.rescanChaptersByManga(mangaId = mangaId)
    }

    // TODO: Fazer um handler de erro melhor
    private fun runLibraryTask(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isIndexing.value = true
            val start = System.currentTimeMillis()
            try {
                block()
            } catch (applicationException: ApplicationException) {
                GlobalErrorHandler.emit(applicationException)
            } catch (exception: Exception) {
                GlobalErrorHandler.emit(exception = GenericInternalException(cause = exception))
            } finally {
                val elapsed = System.currentTimeMillis() - start
                val minTime = 500L
                if (elapsed < minTime) delay(minTime - elapsed)
                _isIndexing.value = false
            }
        }
    }
}