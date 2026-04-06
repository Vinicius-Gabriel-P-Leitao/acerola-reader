package br.acerola.comic.common.viewmodel.archive
import br.acerola.comic.ui.R

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.comic.config.permission.FileSystemAccessManager
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.documentfile.provider.DocumentFile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import br.acerola.comic.config.preference.ComicDirectoryPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

@HiltViewModel
class FileSystemAccessViewModel @Inject constructor(
    private val manager: FileSystemAccessManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    init {
        viewModelScope.launch {
            loadSavedFolder()
        }
    }

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _folderName = MutableStateFlow<String?>(null)
    val folderName: StateFlow<String?> = _folderName.asStateFlow()

    private val _tutorialShown = MutableStateFlow(false)
    val tutorialShown: StateFlow<Boolean> = _tutorialShown.asStateFlow()

    val folderUri get() = manager.folderUri

    fun setTutorialShown(shown: Boolean) {
        viewModelScope.launch {
            ComicDirectoryPreference.setTutorialShown(context, shown)
            _tutorialShown.value = shown
        }
    }

    fun saveFolderUri(uri: Uri?) {
        AcerolaLogger.audit(TAG, "User selected new library folder", LogSource.VIEWMODEL)
        viewModelScope.launch {
            manager.saveFolderUri(uri).onLeft { error -> _uiEvents.send(error) }
            updateFolderName(uri)
            // Se o usuário selecionou uma pasta, marca como tutorial visto
            if (uri != null) setTutorialShown(true)
        }
    }

    suspend fun loadSavedFolder() {
        AcerolaLogger.d(TAG, "Loading saved library folder", LogSource.VIEWMODEL)  
        manager.loadFolderUri()
        updateFolderName(manager.folderUri)
        _tutorialShown.value = ComicDirectoryPreference.tutorialShownFlow(context).first()
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
