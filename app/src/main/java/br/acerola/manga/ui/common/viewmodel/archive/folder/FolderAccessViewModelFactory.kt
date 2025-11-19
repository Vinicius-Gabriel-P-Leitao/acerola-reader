package br.acerola.manga.ui.common.viewmodel.archive.folder

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.acerola.manga.shared.permission.FolderAccessManager

class FolderAccessViewModelFactory(
    private val application: Application,
    private val manager: FolderAccessManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FolderAccessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FolderAccessViewModel(application, manager) as T
        }

        // TODO: Tratar erro de forma melhor
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
