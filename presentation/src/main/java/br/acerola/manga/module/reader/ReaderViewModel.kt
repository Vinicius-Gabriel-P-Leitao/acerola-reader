package br.acerola.manga.module.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.module.reader.state.ReaderUiState
import br.acerola.manga.service.reader.PageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: PageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    fun openChapter(chapter: ChapterFileDto) {
        viewModelScope.launch {
            repository.openChapter(chapter)

            _state.update {
                it.copy(
                    pageCount = repository.pageCount(),
                    pages = emptyMap()
                )
            }
        }
    }

    fun onPageVisible(index: Int) {
        viewModelScope.launch {
            val page = repository.loadPage(index)

            _state.update {
                it.copy(
                    pages = it.pages + (index to page)
                )
            }

            repository.prefetchWindow(center = index)
        }
    }
}
