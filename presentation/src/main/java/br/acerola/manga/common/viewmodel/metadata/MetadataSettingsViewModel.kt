package br.acerola.manga.common.viewmodel.metadata

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.preference.MetadataPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MetadataSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    val generateComicInfo: StateFlow<Boolean> = MetadataPreference.generateComicInfoFlow(context).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setGenerateComicInfo(value: Boolean) {
        viewModelScope.launch {
            MetadataPreference.saveGenerateComicInfo(context, value)
        }
    }
}
