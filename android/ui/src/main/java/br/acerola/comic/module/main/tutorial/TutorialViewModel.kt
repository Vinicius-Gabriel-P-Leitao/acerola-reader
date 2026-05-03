package br.acerola.comic.module.main.tutorial

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.comic.config.preference.OnboardingPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TutorialViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    fun markOnboardingCompleted() {
        viewModelScope.launch {
            OnboardingPreference.markCompleted(context)
        }
    }
}
