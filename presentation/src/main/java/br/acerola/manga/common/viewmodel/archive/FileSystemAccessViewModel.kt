package br.acerola.manga.common.viewmodel.archive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileSystemAccessViewModel @Inject constructor(
    private val manager: FileSystemAccessManager
) : ViewModel() {
    val folderUri get() = manager.folderUri

    fun saveFolderUri(uri: Uri?) {
        AcerolaLogger.audit(TAG, "User selected new library folder", LogSource.VIEWMODEL) // LOG ADICIONADO
        viewModelScope.launch {
            manager.saveFolderUri(uri)
        }
    }

    suspend fun loadSavedFolder() {
        AcerolaLogger.d(TAG, "Loading saved library folder", LogSource.VIEWMODEL) // LOG ADICIONADO
        manager.loadFolderUri()
    }

    companion object {
        private const val TAG = "FileSystemAccessViewModel" // PADRÃO OBRIGATÓRIO
    }
}
