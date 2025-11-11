package br.acerola.manga.ui.common.viewmodel.archive.file

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.shared.config.FileExtension
import br.acerola.manga.shared.config.FilePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilePreferencesViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val context: Context get() = getApplication()

    val selectedExtension: StateFlow<FileExtension> = FilePreferences.fileExtensionFlow(context).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = FileExtension.CBZ
    )

    fun saveExtension(value: FileExtension) {
        viewModelScope.launch {
            FilePreferences.saveFileExtension(context, extension = value)
        }
    }
}
