package br.acerola.manga.ui.common.viewmodel.archive.folder

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.permission.FolderAccessManager
import kotlinx.coroutines.launch

class FolderAccessViewModel(
    application: Application,
    private val manager: FolderAccessManager
) : AndroidViewModel(application) {
    val folderUri get() = manager.folderUri
    var scannedFiles by mutableStateOf<List<Uri>>(value = emptyList())
        private set

    fun saveFolderUri(uri: Uri?) {
        viewModelScope.launch {
            manager.saveFolderUri(uri)
        }
    }

    suspend fun loadSavedFolder() {
        manager.loadFolderUri()
    }
}
