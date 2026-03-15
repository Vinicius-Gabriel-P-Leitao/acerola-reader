package br.acerola.manga.common.viewmodel.library.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.usecase.chapter.GetChaptersUseCase
import br.acerola.manga.usecase.di.ComicInfoCase
import br.acerola.manga.usecase.di.MangadexCase
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
class ChapterRemoteInfoViewModel @Inject constructor(
    @param:MangadexCase private val getMangadexChaptersUseCase: GetChaptersUseCase<ChapterRemoteInfoPageDto>,
    @param:MangadexCase private val rescanMangadexChaptersUseCase: RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto>,
    @param:ComicInfoCase private val rescanComicInfoChaptersUseCase: RescanMangaChaptersUseCase<ChapterRemoteInfoPageDto>,
) : ViewModel() {

    private val _isIndexing = MutableStateFlow(value = false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _chapterPage = MutableStateFlow<ChapterRemoteInfoPageDto?>(value = null)
    val chapterPage: StateFlow<ChapterRemoteInfoPageDto?> = _chapterPage.asStateFlow()

    private val _selectedMangaId = MutableStateFlow<Long?>(value = null)
    val selectedMangaId: StateFlow<Long?> = _selectedMangaId.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var total = 0

    fun init(mangaId: Long, firstPage: ChapterRemoteInfoPageDto) {
        AcerolaLogger.d(TAG, "Initializing with mangaId: $mangaId", LogSource.VIEWMODEL) // LOG ADICIONADO
        _selectedMangaId.value = mangaId
        total = firstPage.total
        currentPage = firstPage.page
        _chapterPage.value = firstPage
    }

    fun loadPage(page: Int) {
        AcerolaLogger.d(TAG, "Loading metadata page: $page", LogSource.VIEWMODEL) // LOG ADICIONADO
        viewModelScope.launch {
            _chapterPage.value = null

            val result: ChapterRemoteInfoPageDto = getMangadexChaptersUseCase.loadPage(
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

    fun syncChaptersByMangadex(mangaId: Long) {
        AcerolaLogger.audit(TAG, "User requested chapter sync from MangaDex", LogSource.VIEWMODEL, mapOf("mangaId" to mangaId.toString())) // LOG ADICIONADO
        viewModelScope.launch {
            _isIndexing.value = true
            rescanMangadexChaptersUseCase(mangaId).handleResult()
            _isIndexing.value = false
        }
    }

    fun syncChaptersByComicInfo(folderId: Long) {
        AcerolaLogger.audit(TAG, "User requested chapter sync from ComicInfo.xml", LogSource.VIEWMODEL, mapOf("folderId" to folderId.toString())) // LOG ADICIONADO
        viewModelScope.launch {
            _isIndexing.value = true
            rescanComicInfoChaptersUseCase(folderId).handleResult()
            _isIndexing.value = false
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            AcerolaLogger.e(TAG, "Metadata operation failed: ${error.uiMessage}", LogSource.VIEWMODEL) // LOG ADICIONADO
            _uiEvents.send(element = error)
        }
    }

    companion object {
        private const val TAG = "ChapterRemoteInfoViewModel" // PADRÃO OBRIGATÓRIO
    }
}
