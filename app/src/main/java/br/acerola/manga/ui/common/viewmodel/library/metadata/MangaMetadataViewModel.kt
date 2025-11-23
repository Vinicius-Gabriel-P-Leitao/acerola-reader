package br.acerola.manga.ui.common.viewmodel.library.metadata

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.exception.ApplicationException
import br.acerola.manga.shared.error.exception.GenericInternalError
import br.acerola.manga.shared.error.handler.GlobalErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MangaMetadataViewModelFactory(
    private val application: Application,
    private val libraryPort: LibraryPort<MangaMetadataDto>,
    private val mangaOperations: LibraryPort.MangaOperations<MangaMetadataDto>,
) : ViewModelProvider.Factory {
    // TODO: Tratar erros melhor
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaMetadataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MangaMetadataViewModel(
                application, libraryPort, mangaOperations
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class MangaMetadataViewModel(
    application: Application,
    private val libraryPort: LibraryPort<MangaMetadataDto>,
    private val mangaOperations: LibraryPort.MangaOperations<MangaMetadataDto>,
) : AndroidViewModel(application) {
    val progress: StateFlow<Int> = libraryPort.progress

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    val metadata: StateFlow<List<MangaMetadataDto>> = mangaOperations.loadMangas().stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = emptyList()
    )

    fun loadAndSyncMangas() = runLibraryTask {
        libraryPort.syncMangas(baseUri = null)
    }

    // TODO: Tratar melhor exceptions, de preferencia de forma personalizada e global
    private fun runLibraryTask(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isIndexing.value = true
            val start = System.currentTimeMillis()
            try {
                block()
            } catch (applicationException: ApplicationException) {
                GlobalErrorHandler.emit(applicationException)
            } catch (exception: Exception) {
                GlobalErrorHandler.emit(
                    exception =
                    GenericInternalError(cause = exception)
                )
            } finally {
                val elapsed = System.currentTimeMillis() - start
                val minTime = 500L
                if (elapsed < minTime) kotlinx.coroutines.delay(timeMillis = minTime - elapsed)
                _isIndexing.value = false
            }
        }
    }
}