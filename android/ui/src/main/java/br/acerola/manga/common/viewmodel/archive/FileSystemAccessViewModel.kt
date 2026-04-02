package br.acerola.manga.common.viewmodel.archive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.documentfile.provider.DocumentFile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class FileSystemAccessViewModel @Inject constructor(
    private val manager: FileSystemAccessManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _folderName = MutableStateFlow<String?>(null)
    val folderName: StateFlow<String?> = _folderName.asStateFlow()

    val folderUri get() = manager.folderUri

    fun saveFolderUri(uri: Uri?) {
        AcerolaLogger.audit(TAG, "User selected new library folder", LogSource.VIEWMODEL)  
        viewModelScope.launch {
            manager.saveFolderUri(uri)
            updateFolderName(uri)
        }
    }

    suspend fun loadSavedFolder() {
        AcerolaLogger.d(TAG, "Loading saved library folder", LogSource.VIEWMODEL)  
        manager.loadFolderUri()
        updateFolderName(manager.folderUri)
    }

    private fun updateFolderName(uri: Uri?) {
        _folderName.value = uri?.let {
            try {
                DocumentFile.fromTreeUri(context, it)?.name
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        private const val TAG = "FileSystemAccessViewModel"  
    }
}
