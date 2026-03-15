package br.acerola.manga.module.manga

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.config.preference.ChapterPerPagePreference
import br.acerola.manga.dto.ChapterDto
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.repository.port.HistoryManagementRepository
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.usecase.chapter.GetChaptersUseCase
import br.acerola.manga.usecase.di.DirectoryCase
import br.acerola.manga.usecase.di.MangadexCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.util.normalizeChapter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.max

@HiltViewModel
class MangaViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>,
    @param:DirectoryCase private val directoryGetChapters: GetChaptersUseCase<ChapterArchivePageDto>,
    @param:MangadexCase private val mangadexGetChapters: GetChaptersUseCase<ChapterRemoteInfoPageDto>,
    private val historyRepository: HistoryManagementRepository
) : ViewModel() {

    private val _selectedChapterPerPage = MutableStateFlow(value = ChapterPageSizeType.SHORT)
    val selectedChapterPerPage: StateFlow<ChapterPageSizeType> = _selectedChapterPerPage.asStateFlow()

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)
    val selectedDirectoryId: StateFlow<Long?> = _selectedDirectoryId.asStateFlow()

    private val _selectedMangaId = MutableStateFlow<Long?>(value = null)
    val selectedMangaId: StateFlow<Long?> = _selectedMangaId.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    val mangaIsIndexing: StateFlow<Boolean> = combine(
        flow = directoryObserve.isIndexing,
        flow2 = mangadexObserve.isIndexing,
    ) { directoryIndexing, remoteInfoIndexing ->
        directoryIndexing || remoteInfoIndexing
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = false
    )

    val chapterIsIndexing: StateFlow<Boolean> = combine(
        flow = directoryGetChapters.isIndexing, flow2 = mangadexGetChapters.isIndexing
    ) { directoryIndexing, remoteInfoIndexing ->
        directoryIndexing || remoteInfoIndexing
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = false
    )

    val mangaProgress: StateFlow<Int> = combine(
        flow = directoryObserve.isIndexing,
        flow2 = directoryObserve.progress,
        flow3 = mangadexObserve.isIndexing,
        flow4 = mangadexObserve.progress
    ) { directoryBusy, directoryProg, remoteInfoBusy, remoteInfoProg ->
        when {
            directoryBusy && directoryProg != -1 -> directoryProg
            remoteInfoBusy && remoteInfoProg != -1 -> remoteInfoProg
            else -> -1
        }
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = -1
    )

    val chapterProgress: StateFlow<Int> = combine(
        flow = directoryGetChapters.isIndexing,
        flow2 = directoryGetChapters.progress,
        flow3 = mangadexGetChapters.isIndexing,
        flow4 = mangadexGetChapters.progress
    ) { directoryBusy, directoryProg, remoteInfoBusy, remoteInfoProg ->
        when {
            directoryBusy && directoryProg != -1 -> directoryProg
            remoteInfoBusy && remoteInfoProg != -1 -> remoteInfoProg
            else -> -1
        }
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = -1
    )

    val manga: StateFlow<MangaDto?> = combine(
        flow = _selectedDirectoryId,
        flow2 = _selectedMangaId,
        flow3 = directoryObserve(),
        flow4 = mangadexObserve(),
    ) { folderId, remoteInfoId, directories, remoteInfos ->
        if (folderId == null) return@combine null
        val directory = directories.find { it.id == folderId } ?: return@combine null

        var remote = remoteInfos.find { it.mangaDirectoryFk == folderId }

        if (remote == null && remoteInfoId != null) {
            remote = remoteInfos.find { it.id == remoteInfoId }
        }

        if (remote == null) {
            val normalizedName = directory.name.normalizeKey()
            remote = remoteInfos.find { it.title.normalizeKey() == normalizedName }
        }

        MangaDto(directory = directory, remoteInfo = remote)
    }.stateIn(
        initialValue = null,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<ReadingHistoryDto?> = _selectedDirectoryId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else historyRepository.getHistoryByMangaId(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val readChapters: StateFlow<List<Long>> = _selectedDirectoryId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else historyRepository.getReadChaptersByMangaId(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<ChapterDto?> = _selectedDirectoryId.flatMapLatest { folderId ->
        if (folderId == null) return@flatMapLatest flowOf(null)

        val localFlow = directoryGetChapters.observeByManga(folderId)
        val remoteFlow = manga.flatMapLatest {
            val mangaId = it?.remoteInfo?.id

            if (mangaId != null) {
                mangadexGetChapters.observeByManga(mangaId)
            } else {
                flowOf(ChapterRemoteInfoPageDto(emptyList(), 0, 0, 0))
            }
        }

        combine(
            localFlow,
            remoteFlow,
            _currentPage,
            _selectedChapterPerPage
        ) { localAll, remoteAll, page, pageSizeType ->
            val items = localAll.items
            if (items.isEmpty()) return@combine ChapterDto(localAll, remoteAll)

            val total = items.size
            val pageSize = pageSizeType.key.toInt()
            val totalPages = ceil(total.toDouble() / pageSize).toInt()
            val safePage = page.coerceIn(0, max(0, totalPages - 1))

            val start = safePage * pageSize
            val end = (start + pageSize).coerceIn(0, total)

            val pagedLocalItems = if (start < total) items.subList(start, end) else emptyList()

            // Match em memória usando normalizeChapter para ignorar variações de string (ex: 0.1 vs 0.01)
            val remoteMap = remoteAll.items.associateBy { it.chapter.normalizeChapter() }

            val filteredRemoteItems = pagedLocalItems.mapNotNull { local ->
                remoteMap[local.chapterSort.normalizeChapter()]
            }

            ChapterDto(
                archive = ChapterArchivePageDto(pagedLocalItems, pageSize, safePage, total),
                remoteInfo = ChapterRemoteInfoPageDto(filteredRemoteItems, pageSize, safePage, total)
            )
        }
    }.stateIn(
        initialValue = null,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    )

    fun init(folderId: Long, mangaId: Long?) {
        AcerolaLogger.audit(TAG, "Initializing MangaScreen", LogSource.VIEWMODEL, mapOf("folderId" to folderId.toString(), "mangaId" to mangaId.toString())) // LOG ADICIONADO
        _selectedDirectoryId.value = folderId
        _selectedMangaId.value = mangaId

        viewModelScope.launch {
            ChapterPerPagePreference.chapterPerPageFlow(context).collect { size ->
                _selectedChapterPerPage.value = size
            }
        }

        // Sincroniza o ID remoto interno caso o StateFlow 'manga' encontre um novo vínculo
        viewModelScope.launch {
            manga.collect { mangaDto ->
                val newRemoteId = mangaDto?.remoteInfo?.id
                if (newRemoteId != null && newRemoteId != _selectedMangaId.value) {
                    AcerolaLogger.d(TAG, "Syncing remote ID: $newRemoteId", LogSource.VIEWMODEL) // LOG ADICIONADO
                    _selectedMangaId.value = newRemoteId
                }
            }
        }
    }

    fun updateChapterPerPage(size: ChapterPageSizeType) {
        if (_selectedChapterPerPage.value == size) return
        AcerolaLogger.d(TAG, "Changing chapter page size to: ${size.name}", LogSource.VIEWMODEL) // LOG ADICIONADO
        _selectedChapterPerPage.value = size
        viewModelScope.launch {
            ChapterPerPagePreference.saveChapterPerPage(context, size)
        }
    }

    fun loadPageAsync(page: Int) {
        AcerolaLogger.d(TAG, "Loading chapter list page: $page", LogSource.VIEWMODEL) // LOG ADICIONADO
        _currentPage.value = page
    }

    fun toggleChapterReadStatus(chapterId: Long) {
        val mangaId = _selectedDirectoryId.value ?: return
        val isRead = readChapters.value.contains(chapterId)

        AcerolaLogger.audit(TAG, "Toggling chapter read status", LogSource.VIEWMODEL, mapOf("chapterId" to chapterId.toString(), "newStatus" to (!isRead).toString())) // LOG ADICIONADO

        viewModelScope.launch {
            if (isRead) {
                historyRepository.unmarkChapterAsRead(chapterId)
            } else {
                historyRepository.markChapterAsRead(mangaId, chapterId)
            }
        }
    }

    private fun String.normalizeKey(): String {
        return this.filter { it.isLetterOrDigit() }.lowercase()
    }

    companion object {
        private const val TAG = "MangaViewModel" // PADRÃO OBRIGATÓRIO
    }
}
