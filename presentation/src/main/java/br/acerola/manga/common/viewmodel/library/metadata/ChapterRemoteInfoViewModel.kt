package br.acerola.manga.common.viewmodel.library.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.repository.port.MangadexFsOps
import br.acerola.manga.util.normalizeChapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChapterRemoteInfoViewModel @Inject constructor(
    @param:MangadexFsOps
    private val mangadexChapterRepository: LibraryRepository.ChapterOperations<ChapterRemoteInfoPageDto>,
) : ViewModel() {
    private val _chapterPage = MutableStateFlow<ChapterRemoteInfoPageDto?>(value = null)
    val chapterPage: StateFlow<ChapterRemoteInfoPageDto?> = _chapterPage.asStateFlow()

    private val _selectedMangaId = MutableStateFlow<Long?>(value = null)
    val selectedMangaId: StateFlow<Long?> = _selectedMangaId.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var total = 0

    fun init(mangaId: Long, firstPage: ChapterRemoteInfoPageDto) {
        _selectedMangaId.value = mangaId
        total = firstPage.total
        currentPage = firstPage.page
        _chapterPage.value = firstPage
    }

    fun loadPage(page: Int) {
        viewModelScope.launch {
            _chapterPage.value = null

            val result: ChapterRemoteInfoPageDto = mangadexChapterRepository.loadChapterPage(
                mangaId = _selectedMangaId.value!!,
                pageSize = pageSize,
                total = total,
                page = page,
            )

            val sortedItems: List<ChapterFeedDto> = result.items.sortedBy {
                it.chapter.normalizeChapter().toFloatOrNull() ?: 0f
            }

            _chapterPage.value = result.copy(items = sortedItems)
        }
    }
}