package br.acerola.manga.common.viewmodel.library.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.repository.port.DirectoryFsOps
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.util.normalizeChapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChapterArchiveViewModel @Inject constructor(
    @param:DirectoryFsOps
    private val chapterFileRepository: LibraryRepository.ChapterOperations<ChapterArchivePageDto>,
) : ViewModel() {
    private val _chapterPage = MutableStateFlow<ChapterArchivePageDto?>(value = null)
    val chapterPage: StateFlow<ChapterArchivePageDto?> = _chapterPage.asStateFlow()

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)
    val selectedDirectoryId: StateFlow<Long?> = _selectedDirectoryId.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var total = 0

    fun init(directoryId: Long, firstPage: ChapterArchivePageDto) {
        _selectedDirectoryId.value = directoryId
        total = firstPage.total
        currentPage = firstPage.page
        _chapterPage.value = firstPage
    }

    fun loadPage(page: Int) {
        viewModelScope.launch {
            _chapterPage.value = null

            val result: ChapterArchivePageDto = chapterFileRepository.loadChapterPage(
                mangaId = _selectedDirectoryId.value!!,
                pageSize = pageSize,
                total = total,
                page = page,
            )

            val sortedItems: List<ChapterFileDto> = result.items.sortedBy {
                it.chapterSort.normalizeChapter().toFloatOrNull() ?: 0f
            }

            _chapterPage.value = result.copy(items = sortedItems)
        }
    }
}
