package br.acerola.manga.common.viewmodel.library.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.ChapterPageDto
import br.acerola.manga.repository.port.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChapterArchiveViewModel @Inject constructor(
    private val chapterOperations: LibraryRepository.ChapterOperations<ChapterPageDto>,
) : ViewModel() {
    private val _chapterPage = MutableStateFlow<ChapterPageDto?>(value = null)
    val chapterPage: StateFlow<ChapterPageDto?> = _chapterPage.asStateFlow()

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)
    val selectedFolderId: StateFlow<Long?> = _selectedDirectoryId.asStateFlow()

    private val pageSize = 20
    private var currentPage = 0
    private var total = 0

    fun init(directoryId: Long, firstPage: ChapterPageDto) {
        _selectedDirectoryId.value = directoryId
        total = firstPage.total
        currentPage = firstPage.page
        _chapterPage.value = firstPage
    }

    fun loadNextPage() {
        _selectedDirectoryId.value ?: return
        if ((currentPage + 1) * pageSize >= total) return

        currentPage++
        loadPage(currentPage)
    }

    fun loadPage(page: Int) {
        viewModelScope.launch {
            _chapterPage.value = null

            val result: ChapterPageDto = chapterOperations.loadNextPage(
                folderId = _selectedDirectoryId.value!!,
                pageSize = pageSize,
                page = page,
                total = total,
            )

            val sortedItems: List<ChapterFileDto> = result.items.sortedBy {
                it.chapterSort.replace(oldChar = ',', newChar = '.').toFloatOrNull() ?: 0f
            }

            _chapterPage.value = result.copy(items = sortedItems)
        }
    }
}

