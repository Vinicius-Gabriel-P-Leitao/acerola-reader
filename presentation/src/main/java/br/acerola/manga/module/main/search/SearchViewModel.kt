package br.acerola.manga.module.main.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.error.UserMessage
import br.acerola.manga.module.main.search.state.SearchAction
import br.acerola.manga.module.main.search.state.SearchUiState
import br.acerola.manga.usecase.search.SearchMangaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val searchMangaUseCase: SearchMangaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> _uiState.update { it.copy(query = action.query) }
            is SearchAction.Search -> search()
        }
    }

    private fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isLoading = true, searchResults = emptyList()) }

        viewModelScope.launch {
            searchMangaUseCase.search(query).fold(
                ifLeft = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvents.send(UserMessage.Raw(error.uiMessage.asString(context)))
                },
                ifRight = { mangas ->
                    _uiState.update { it.copy(isLoading = false, searchResults = mangas) }
                }
            )
        }
    }
}
