package br.acerola.manga.ui.feature.main.home.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.acerola.manga.domain.service.archive.ArchiveMangaService
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel

class MangaLibraryViewModelFactory(
    private val application: Application,
    private val archiveService: ArchiveMangaService,
    private val folderAccessViewModel: FolderAccessViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaLibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaLibraryViewModel(application, archiveService, folderAccessViewModel) as T
        }

        // TODO: Tratar erro de forma melhor
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}