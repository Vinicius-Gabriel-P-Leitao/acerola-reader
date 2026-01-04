package br.acerola.manga.common.viewmodel.archive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.permission.FileSystemAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileSystemAccessViewModel @Inject constructor(
    private val manager: FileSystemAccessManager
) : ViewModel() {
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