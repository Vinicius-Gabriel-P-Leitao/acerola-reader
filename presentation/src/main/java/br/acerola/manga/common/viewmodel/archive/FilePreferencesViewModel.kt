package br.acerola.manga.common.viewmodel.archive

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.config.preference.FilePreferences
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilePreferencesViewModel @Inject constructor(
    private val application: Application,
) : ViewModel() {
    private val context: Context get() = application

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    val selectedExtension: StateFlow<FileExtension> = FilePreferences.fileExtensionFlow(context)
        .onEach { AcerolaLogger.d(TAG, "Selected extension updated: $it", LogSource.VIEWMODEL) }  
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = FileExtension.CBZ
        )

    fun saveExtension(value: FileExtension) {
        AcerolaLogger.audit(TAG, "User saving file extension preference: $value", LogSource.VIEWMODEL)  
        viewModelScope.launch {
            FilePreferences.saveFileExtension(context, extension = value)
        }
    }

    companion object {
        private const val TAG = "FilePreferencesViewModel"  
    }
}
