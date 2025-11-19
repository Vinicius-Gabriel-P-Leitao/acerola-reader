package br.acerola.manga.ui.common.viewmodel.archive.folder

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.shared.permission.FolderAccessManager
import kotlinx.coroutines.launch

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
