package br.acerola.manga.ui.common.viewmodel.library.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.exception.ApplicationException
import br.acerola.manga.shared.error.exception.GenericInternalException
import br.acerola.manga.shared.error.handler.GlobalErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaMetadataViewModel @Inject constructor(
    private val libraryPort: LibraryPort<MangaMetadataDto>,
    private val mangaOperations: LibraryPort.MangaOperations<MangaMetadataDto>,
) : ViewModel() {
    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    val progress: StateFlow<Int> = libraryPort.progress


    val metadata: StateFlow<List<MangaMetadataDto>> = mangaOperations.loadMangas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun syncLibrary() = runLibraryTask {
        libraryPort.syncMangas(baseUri = null)
    }

    fun rescanMangas() = runLibraryTask {
        libraryPort.rescanMangas(baseUri = null)
    }

    fun deepScanLibrary() = runLibraryTask {
        libraryPort.deepRescanLibrary(baseUri = null)
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
                if (elapsed < minTime) kotlinx.coroutines.delay(minTime - elapsed)
                _isIndexing.value = false
            }
        }
    }
}