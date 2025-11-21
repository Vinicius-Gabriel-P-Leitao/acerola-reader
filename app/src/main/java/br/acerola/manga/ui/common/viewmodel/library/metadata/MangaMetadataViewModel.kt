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
    private val _mangas = MutableStateFlow<List<MangaMetadataDto>>(value = emptyList())
    val mangas: StateFlow<List<MangaMetadataDto>> get() = _mangas.asStateFlow()

    val metadata: StateFlow<List<MangaMetadataDto>> = mangaOperations.loadMangas().stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = emptyList()
    )

    fun loadAndSyncMangas() {
        viewModelScope.launch {
            try {
                libraryPort.syncMangas(baseUri = null)
            } catch (applicationException: ApplicationException) {
                GlobalErrorHandler.emit(applicationException)
            } catch (exception: Exception) {
                GlobalErrorHandler.emit(
                    exception =
                        GenericInternalError(cause = exception)
                )
            }
        }
    }
}