package br.acerola.comic.common.viewmodel.theme
import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.comic.config.preference.AppTheme
import br.acerola.comic.config.preference.ThemePreference
import br.acerola.comic.error.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel
    @Inject
    constructor(
        private val application: Application,
    ) : ViewModel() {
        private val context: Context get() = application

        private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
        val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

        val currentTheme: StateFlow<AppTheme> =
            ThemePreference
                .themeFlow(context)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = AppTheme.CATPPUCCIN,
                )

        fun setTheme(theme: AppTheme) {
            viewModelScope.launch {
                ThemePreference.saveTheme(context, theme)
            }
        }
    }
