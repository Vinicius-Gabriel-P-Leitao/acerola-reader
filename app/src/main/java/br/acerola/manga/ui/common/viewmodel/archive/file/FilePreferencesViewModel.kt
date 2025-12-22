package br.acerola.manga.ui.common.viewmodel.archive.file

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.shared.config.preference.FileExtension
import br.acerola.manga.shared.config.preference.FilePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilePreferencesViewModel @Inject constructor(
    private val application: Application,
) : ViewModel() {
    private val context: Context get() = application

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
