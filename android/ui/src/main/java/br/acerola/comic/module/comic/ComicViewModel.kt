package br.acerola.comic.module.comic
import br.acerola.comic.ui.R

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.comic.config.preference.ChapterPageSizeType
import br.acerola.comic.config.preference.ChapterPerPagePreference
import br.acerola.comic.config.preference.ChapterSortPreference
import br.acerola.comic.config.preference.ChapterSortPreferenceData
import br.acerola.comic.config.preference.ChapterSortType
import br.acerola.comic.config.preference.SortDirection
import br.acerola.comic.usecase.DirectoryCase
import br.acerola.comic.usecase.MangadexCase
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.history.ObserveComicHistoryUseCase
import br.acerola.comic.usecase.history.TrackReadingProgressUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.util.normalizeChapter
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
class ComicViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ObserveComicHistoryUseCase: ObserveComicHistoryUseCase,
    private val trackReadingProgressUseCase: TrackReadingProgressUseCase,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<ComicMetadataDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<ComicDirectoryDto>,
    @param:DirectoryCase private val directoryGetChapters: ObserveChaptersUseCase<ChapterArchivePageDto>,
    @param:MangadexCase private val mangadexGetChapters: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>,
    private val manageCategoriesUseCase: ManageCategoriesUseCase,
) : ViewModel() {

    private val _selectedChapterPerPage = MutableStateFlow(value = ChapterPageSizeType.SHORT)
    val selectedChapterPerPage: StateFlow<ChapterPageSizeType> = _selectedChapterPerPage.asStateFlow()

    private val _chapterSortSettings = MutableStateFlow(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))
    val chapterSortSettings: StateFlow<ChapterSortPreferenceData> = _chapterSortSettings.asStateFlow()

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)

    private val _selectedMangaId = MutableStateFlow<Long?>(value = null)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val manga: StateFlow<ComicDto?> = combine(
        _selectedDirectoryId,
        _selectedMangaId,
        directoryObserve(),
        mangadexObserve(),
        _selectedDirectoryId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else manageCategoriesUseCase.getCategoryByMangaId(id)
        }
    ) { folderId, remoteInfoId, directories, remoteInfos, category ->
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

        ComicDto(directory = directory, remoteInfo = remote, category = category)
    }.stateIn(
        initialValue = null,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<ReadingHistoryDto?> = _selectedDirectoryId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else ObserveComicHistoryUseCase.observeByManga(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val readChapters: StateFlow<List<Long>> = _selectedDirectoryId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else ObserveComicHistoryUseCase.observeReadChapters(id)
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
            _selectedChapterPerPage,
            _chapterSortSettings
        ) { localAll, remoteAll, page, pageSizeType, sort ->
            val items = localAll.items
            if (items.isEmpty()) return@combine ChapterDto(localAll, remoteAll)

            // Apply Sorting
            val sortedItems = when (sort.type) {
                ChapterSortType.NUMBER -> items.sortedWith(
                    compareBy(
                        { it.chapterSort.substringBefore('.').toIntOrNull() ?: 0 },
                        { it.chapterSort.substringAfter('.', "0").toIntOrNull() ?: 0 }
                    )
                )
                ChapterSortType.LAST_UPDATE -> items.sortedBy { it.lastModified }
            }

            val finalItems = if (sort.direction == SortDirection.DESCENDING) sortedItems.reversed() else sortedItems

            val total = finalItems.size
            val pageSize = pageSizeType.key.toInt()
            val totalPages = ceil(total.toDouble() / pageSize).toInt()
            val safePage = page.coerceIn(0, max(0, totalPages - 1))

            val start = safePage * pageSize
            val end = (start + pageSize).coerceIn(0, total)

            val pagedLocalItems = if (start < total) finalItems.subList(start, end) else emptyList()

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
        AcerolaLogger.audit(
            TAG, "Initializing MangaScreen", LogSource.VIEWMODEL,
            mapOf("folderId" to folderId.toString(), "mangaId" to mangaId.toString())
        )
        _selectedDirectoryId.value = folderId
        _selectedMangaId.value = mangaId

        viewModelScope.launch {
            ChapterPerPagePreference.chapterPerPageFlow(context).collect { size ->
                _selectedChapterPerPage.value = size
            }
        }

        viewModelScope.launch {
            ChapterSortPreference.sortFlow(context).collect { sort ->
                _chapterSortSettings.value = sort
            }
        }

        viewModelScope.launch {
            manga.collect { mangaDto ->
                val newRemoteId = mangaDto?.remoteInfo?.id
                if (newRemoteId != null && newRemoteId != _selectedMangaId.value) {
                    AcerolaLogger.d(TAG, "Syncing remote ID: $newRemoteId", LogSource.VIEWMODEL)
                    _selectedMangaId.value = newRemoteId
                }
            }
        }
    }

    fun updateChapterPerPage(size: ChapterPageSizeType) {
        if (_selectedChapterPerPage.value == size) return
        AcerolaLogger.d(TAG, "Changing chapter page size to: ${size.name}", LogSource.VIEWMODEL)
        _selectedChapterPerPage.value = size
        viewModelScope.launch {
            ChapterPerPagePreference.saveChapterPerPage(context, size)
        }
    }

    fun updateChapterSort(sort: ChapterSortPreferenceData) {
        _chapterSortSettings.value = sort
        viewModelScope.launch {
            ChapterSortPreference.saveSort(context, sort)
        }
    }

    fun loadPageAsync(page: Int) {
        AcerolaLogger.d(TAG, "Loading chapter list page: $page", LogSource.VIEWMODEL)
        _currentPage.value = page
    }

    fun toggleChapterReadStatus(chapterId: Long) {
        val mangaId = _selectedDirectoryId.value ?: return
        val isRead = readChapters.value.contains(chapterId)

        AcerolaLogger.audit(
            TAG, "Toggling chapter read status", LogSource.VIEWMODEL,
            mapOf("chapterId" to chapterId.toString(), "newStatus" to (!isRead).toString())
        )

        viewModelScope.launch {
            trackReadingProgressUseCase.toggleReadStatus(mangaId, chapterId, isRead)
        }
    }

    private fun String.normalizeKey(): String {
        return this.filter { it.isLetterOrDigit() }.lowercase()
    }

    companion object {

        private const val TAG = "MangaViewModel"
    }
}
