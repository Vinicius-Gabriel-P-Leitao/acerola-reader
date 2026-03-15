package br.acerola.manga.common.viewmodel.theme

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.preference.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {
    private val context: Context get() = application

    val useDynamicColor: StateFlow<Boolean> = ThemePreference.dynamicColorFlow(context)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = false
        )

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreference.saveDynamicColor(context, enabled)
        }
    }
}
