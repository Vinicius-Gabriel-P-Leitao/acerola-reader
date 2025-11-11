package br.acerola.manga.ui.common.viewmodel.library

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel

class MangaLibraryViewModelFactory(
    private val application: Application,
    private val libraryPort: LibraryPort,
    private val folderAccessViewModel: FolderAccessViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaLibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaLibraryViewModel(application, libraryPort, folderAccessViewModel) as T
        }

        // TODO: Tratar erro de forma melhor
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}