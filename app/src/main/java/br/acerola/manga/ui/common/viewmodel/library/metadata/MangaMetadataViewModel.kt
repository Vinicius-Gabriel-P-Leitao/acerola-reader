package br.acerola.manga.ui.common.viewmodel.library.metadata

import android.app.Application
import android.net.Uri
import androidx.annotation.Nullable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.database.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.database.dao.database.metadata.MangaMetadataDao
import br.acerola.manga.domain.mapper.toDto
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.domain.service.library.sync.SyncMetadataMangaService
import br.acerola.manga.domain.service.mangadex.FetchMangaDataMangaDexService
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MangaMetadataViewModelFactory(
    private val application: Application,
    private val libraryPort: LibraryPort<MangaMetadataDto>,
) : ViewModelProvider.Factory {
    // TODO: Tratar erros melhor
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaMetadataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MangaMetadataViewModel(
                application, libraryPort,
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class MangaMetadataViewModel(
    application: Application,
    private val libraryPort: LibraryPort<MangaMetadataDto>,
) : AndroidViewModel(application) {
    val progress: StateFlow<Int> = libraryPort.progress

    private val _mangas = MutableStateFlow<List<MangaMetadataDto>>(emptyList())
    val mangas: StateFlow<List<MangaMetadataDto>> get() = _mangas.asStateFlow()

    fun loadAndSyncMangas() {
        viewModelScope.launch {
            libraryPort.syncMangas(null)
            println(mangas.value)
        }
    }
}