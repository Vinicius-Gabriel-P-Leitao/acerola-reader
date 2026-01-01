package br.acerola.manga.common.viewmodel.library.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.error.exception.ApplicationException
import br.acerola.manga.error.exception.GenericInternalException
import br.acerola.manga.error.handler.GlobalErrorHandler
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.repository.port.LibraryRepository
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
class MangaMetadataViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository<MangaMetadataDto>,
    private val mangaOperations: LibraryRepository.MangaOperations<MangaMetadataDto>,
) : ViewModel() {
    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    val progress: StateFlow<Int> = libraryRepository.progress


    val metadata: StateFlow<List<MangaMetadataDto>> = mangaOperations.loadMangas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun syncLibrary() = runLibraryTask {
        libraryRepository.syncMangas(baseUri = null)
    }

    fun rescanMangas() = runLibraryTask {
        libraryRepository.rescanMangas(baseUri = null)
    }

    fun deepScanLibrary() = runLibraryTask {
        libraryRepository.deepRescanLibrary(baseUri = null)
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