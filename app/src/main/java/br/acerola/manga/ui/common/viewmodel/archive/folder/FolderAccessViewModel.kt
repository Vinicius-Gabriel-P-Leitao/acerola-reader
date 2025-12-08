package br.acerola.manga.ui.common.viewmodel.archive.folder

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.acerola.manga.shared.permission.FolderAccessManager
import kotlinx.coroutines.launch


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

class FolderAccessViewModel(
    application: Application,
    private val manager: FolderAccessManager
) : AndroidViewModel(application) {
    val folderUri get() = manager.folderUri

    fun saveFolderUri(uri: Uri?) {
        viewModelScope.launch {
            manager.saveFolderUri(uri)
        }
    }

    suspend fun loadSavedFolder() {
        manager.loadFolderUri()
    }
}
