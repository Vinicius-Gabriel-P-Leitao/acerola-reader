package br.acerola.manga.module.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import br.acerola.manga.dto.ChapterDto
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.repository.port.DirectoryFsOps
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.repository.port.MangadexFsOps
import br.acerola.manga.util.normalizeChapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaViewModel @Inject constructor(
    @param:DirectoryFsOps private val mangaDirectoryOperation: LibraryRepository.MangaOperations<MangaDirectoryDto>,

    @param:MangadexFsOps private val mangadexRemoteInfoOperation: LibraryRepository.MangaOperations<MangaRemoteInfoDto>,

    @param:DirectoryFsOps private val chapterArchiveOperation: LibraryRepository.ChapterOperations<ChapterArchivePageDto>,

    @param:MangadexFsOps private val chapterRemoteInfoOperation: LibraryRepository.ChapterOperations<ChapterRemoteInfoPageDto>,
) : ViewModel() {
    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)
    val selectedDirectoryId: StateFlow<Long?> = _selectedDirectoryId.asStateFlow()

    private val _selectedMangaId = MutableStateFlow<Long?>(value = null)
    val selectedMangaId: StateFlow<Long?> = _selectedMangaId.asStateFlow()

    private val _chapter = MutableStateFlow<ChapterDto?>(value = null)
    val chapters: StateFlow<ChapterDto?> = _chapter.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    val isIndexing: StateFlow<Boolean> = combine(
        flow = mangaDirectoryOperation.isIndexing, flow2 = mangadexRemoteInfoOperation.isIndexing
    ) { directoryIndexing, remoteInfoIndexing ->
        directoryIndexing || remoteInfoIndexing
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = false
    )

    val progress: StateFlow<Int> = combine(
        flow = mangaDirectoryOperation.isIndexing,
        flow2 = mangaDirectoryOperation.progress,
        flow3 = mangadexRemoteInfoOperation.isIndexing,
        flow4 = mangadexRemoteInfoOperation.progress
    ) { directoryBusy, directoryProg, remoteInfoBusy, remoteInfoProg ->
        when {
            directoryBusy && directoryProg != -1 -> directoryProg
            remoteInfoBusy && remoteInfoProg != -1 -> remoteInfoProg
            else -> -1
        }
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = -1
    )

    // TODO: Transformar em config do DataStore
    private var currentPage = 0
    private val pageSize = 20
    private var total = 0

    fun init(folderId: Long, mangaId: Long?) {
        _selectedDirectoryId.value = folderId
        _selectedMangaId.value = mangaId

        viewModelScope.launch {
            loadPage(page = 0)
        }
    }

    fun loadPageAsync(page: Int) {
        viewModelScope.launch {
            loadPage(page)
        }
    }

    // WARN: Só no mangadex tem rota para dados tão detalhados para capitulos
    private suspend fun loadPage(page: Int) {
        val folderId = _selectedDirectoryId.value ?: return
        val mangaId = _selectedMangaId.value

        currentPage = page

        if (total == 0) {
            total = chapterArchiveOperation.loadPage(mangaId = folderId, total = 0, page = 0, pageSize = pageSize).total
        }

        val localPage = chapterArchiveOperation.loadPage(
            mangaId = folderId, total = total, page = page, pageSize = pageSize
        )

        val remotePage = mangaId?.let {
            chapterRemoteInfoOperation.loadPage(
                mangaId = it, total = total, page = page, pageSize = pageSize
            )
        } ?: ChapterRemoteInfoPageDto(
            items = emptyList(), pageSize = pageSize, page = page, total = total
        )

        val remoteMap = remotePage.items.associateBy { it.chapter.normalizeChapter() }

        val filteredRemoteItems = localPage.items.mapNotNull {
            remoteMap[it.chapterSort.normalizeChapter()]
        }

        _chapter.value = ChapterDto(
            archive = localPage, remoteInfo = remotePage.copy(items = filteredRemoteItems)
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadPageAllChapters(
        folderId: Long, mangaId: Long?
    ): Flow<ChapterDto> {
        return combine(flow = chapterArchiveOperation.loadChapterByManga(mangaId = folderId), flow2 = mangaId?.let {
            chapterRemoteInfoOperation.loadChapterByManga(mangaId = it)
        } ?: flowOf(
            value = ChapterRemoteInfoPageDto(
                items = emptyList(),
                pageSize = 0,
                total = 0,
                page = 0,
            )
        )) { local, remote ->

            val remoteMap = remote.items.associateBy { it.chapter.normalizeChapter() }

            val filteredRemoteItems = local.items.mapNotNull {
                remoteMap[it.chapterSort.normalizeChapter()]
            }

            ChapterDto(
                archive = local, remoteInfo = remote.copy(items = filteredRemoteItems)
            )
        }
    }

    private suspend fun <T> Either<UserMessage, T>.handleResult() {
        this.onLeft { error ->
            _uiEvents.send(element = error)
        }
    }
}