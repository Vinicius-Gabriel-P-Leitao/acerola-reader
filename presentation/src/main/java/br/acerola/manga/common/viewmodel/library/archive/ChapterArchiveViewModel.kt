package br.acerola.manga.common.viewmodel.library.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.usecase.chapter.GetChaptersUseCase
import br.acerola.manga.usecase.di.DirectoryCase
import br.acerola.manga.usecase.manga.RescanMangaChaptersUseCase
import br.acerola.manga.util.normalizeChapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChapterArchiveViewModel @Inject constructor(
    @param:DirectoryCase private val getChaptersUseCase: GetChaptersUseCase<ChapterArchivePageDto>,
    @param:DirectoryCase private val rescanMangaChaptersUseCase: RescanMangaChaptersUseCase<ChapterArchivePageDto>,
) : ViewModel() {

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _chapterPage = MutableStateFlow<ChapterArchivePageDto?>(value = null)
    val chapterPage: StateFlow<ChapterArchivePageDto?> = _chapterPage.asStateFlow()

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)
    val selectedDirectoryId: StateFlow<Long?> = _selectedDirectoryId.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var total = 0

    fun init(directoryId: Long, firstPage: ChapterArchivePageDto) {
        AcerolaLogger.d(TAG, "Initializing with directoryId: $directoryId", LogSource.VIEWMODEL) // LOG ADICIONADO
        _selectedDirectoryId.value = directoryId
        total = firstPage.total
        currentPage = firstPage.page
        _chapterPage.value = firstPage
    }

    fun loadPage(page: Int) {
        AcerolaLogger.d(TAG, "Loading local chapter page: $page", LogSource.VIEWMODEL) // LOG ADICIONADO
        viewModelScope.launch {
            _chapterPage.value = null

            val result: ChapterArchivePageDto = getChaptersUseCase.loadPage(
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

    fun syncChaptersByMangaDirectory(folderId: Long) {
        AcerolaLogger.audit(TAG, "User requested local chapter rescan", LogSource.VIEWMODEL, mapOf("folderId" to folderId.toString())) // LOG ADICIONADO
        viewModelScope.launch {
            _isIndexing.value = true
            rescanMangaChaptersUseCase(mangaId = folderId).handleResult()
            _isIndexing.value = false
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            AcerolaLogger.e(TAG, "Local chapter operation failed: ${error.uiMessage}", LogSource.VIEWMODEL) // LOG ADICIONADO
            _uiEvents.send(element = error)
        }
    }

    companion object {
        private const val TAG = "ChapterArchiveViewModel" // PADRÃO OBRIGATÓRIO
    }
}
