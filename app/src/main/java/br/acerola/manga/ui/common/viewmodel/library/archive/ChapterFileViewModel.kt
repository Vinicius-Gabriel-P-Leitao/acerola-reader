package br.acerola.manga.ui.common.viewmodel.library.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.dto.archive.ChapterPageDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChapterFileViewModel @Inject constructor(
    private val chapterOperations: LibraryPort.ChapterOperations<ChapterPageDto>,
) : ViewModel() {
    private val _chapterPage = MutableStateFlow<ChapterPageDto?>(value = null)
    val chapterPage: StateFlow<ChapterPageDto?> = _chapterPage.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(value = null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val pageSize = 20
    private var currentPage = 0
    private var total = 0

    fun init(folderId: Long, firstPage: ChapterPageDto) {
        _selectedFolderId.value = folderId
        total = firstPage.total
        currentPage = firstPage.page
        _chapterPage.value = firstPage
    }

    fun loadNextPage() {
        _selectedFolderId.value ?: return
        if ((currentPage + 1) * pageSize >= total) return

        currentPage++
        loadPage(currentPage)
    }

    private fun loadPage(page: Int) {
        viewModelScope.launch {
            val result = chapterOperations.loadNextPage(
                folderId = _selectedFolderId.value!!,
                pageSize = pageSize,
                page = page,
                total = total,
            )

            val merged = if (page == 0) {
                result.items
            } else {
                (_chapterPage.value?.items ?: emptyList()) + result.items
            }

            _chapterPage.value = result.copy(items = merged)
        }
    }
}

