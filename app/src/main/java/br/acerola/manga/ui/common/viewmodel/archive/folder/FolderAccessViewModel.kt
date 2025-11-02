package br.acerola.manga.ui.common.viewmodel.archive.folder

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.permission.FolderAccessManager
import br.acerola.manga.domain.service.archive.scanFolder
import kotlinx.coroutines.launch

class FolderAccessViewModel(
    application: Application,
    private val manager: FolderAccessManager
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>()

    val folderUri get() = manager.folderUri
    var scannedFiles by mutableStateOf<List<Uri>>(emptyList())
        private set

    fun saveFolderUri(uri: Uri?) {
        viewModelScope.launch {
            manager.saveFolderUri(uri)
            folderUri?.let {
                scannedFiles = scanFolder(context, it)
            }
        }
    }

    suspend fun loadSavedFolder() {
        manager.loadFolderUri()
    }
}
