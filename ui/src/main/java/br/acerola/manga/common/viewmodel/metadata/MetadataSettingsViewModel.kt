package br.acerola.manga.common.viewmodel.metadata

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.preference.MetadataPreference
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
class MetadataSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    val generateComicInfo: StateFlow<Boolean> = MetadataPreference.generateComicInfoFlow(context)
        .onEach { AcerolaLogger.d(TAG, "Generate ComicInfo preference updated: $it", LogSource.VIEWMODEL) }  
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setGenerateComicInfo(value: Boolean) {
        AcerolaLogger.audit(TAG, "User setting Generate ComicInfo preference: $value", LogSource.VIEWMODEL)  
        viewModelScope.launch {
            MetadataPreference.saveGenerateComicInfo(context, value)
        }
    }

    companion object {
        private const val TAG = "MetadataSettingsViewModel"  
    }
}
