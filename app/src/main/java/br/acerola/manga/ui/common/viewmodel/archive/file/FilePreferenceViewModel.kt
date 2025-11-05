package br.acerola.manga.ui.common.viewmodel.archive.file

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.shared.config.FilePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FilePreferencesViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val context: Context get() = getApplication()

    private val _selectedExtension = MutableStateFlow<String?>(value = null)
    val selectedExtension: StateFlow<String?> = _selectedExtension

    init {
        viewModelScope.launch {
            FilePreferences.fileExtensionFlow(context).collectLatest { value ->
                _selectedExtension.value = value
            }
        }
    }

    fun saveExtension(value: String) {
        viewModelScope.launch {
            FilePreferences.saveFileExtension(context, value)
        }
    }
}
