package br.acerola.comic.module.comic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.comic.config.preference.ChapterPerPagePreference
import br.acerola.comic.config.preference.ChapterSortPreference
import br.acerola.comic.config.preference.VolumeViewPreference
import br.acerola.comic.config.preference.types.ChapterPageSizeType
import br.acerola.comic.config.preference.types.ChapterSortPreferenceData
import br.acerola.comic.config.preference.types.ChapterSortType
import br.acerola.comic.config.preference.types.SortDirection
import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.usecase.DirectoryCase
import br.acerola.comic.usecase.MangadexCase
import br.acerola.comic.usecase.chapter.ObserveCombinedChaptersUseCase
import br.acerola.comic.usecase.chapter.ObserveVolumeChaptersUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.history.ObserveComicHistoryUseCase
import br.acerola.comic.usecase.history.TrackReadingProgressUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComicViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val observeComicHistoryUseCase: ObserveComicHistoryUseCase,
    private val trackReadingProgressUseCase: TrackReadingProgressUseCase,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<ComicMetadataDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<ComicDirectoryDto>,
    private val observeChaptersUseCase: ObserveCombinedChaptersUseCase,
    @param:DirectoryCase private val directoryObserveVolumeChapters: ObserveVolumeChaptersUseCase,
    private val manageCategoriesUseCase: ManageCategoriesUseCase,
) : ViewModel() {

    private val selectedDirectoryId = MutableStateFlow<Long?>(null)
    private val selectedComicId = MutableStateFlow<Long?>(null)

    private val _selectedChapterPerPage = MutableStateFlow(ChapterPageSizeType.SHORT)
    val selectedChapterPerPage: StateFlow<ChapterPageSizeType> = _selectedChapterPerPage.asStateFlow()

    private val _chapterSortSettings = MutableStateFlow(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))
    val chapterSortSettings: StateFlow<ChapterSortPreferenceData> = _chapterSortSettings.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _volumeViewMode = MutableStateFlow(VolumeViewType.CHAPTER)
    val volumeViewMode: StateFlow<VolumeViewType> = _volumeViewMode.asStateFlow()

    private val _activeVolumeId = MutableStateFlow<Long?>(null)
    val activeVolumeId: StateFlow<Long?> = _activeVolumeId.asStateFlow()

    private val _volumeSectionOverrides = MutableStateFlow<Map<Long, VolumeChapterGroupDto>>(emptyMap())
    val volumeSectionOverrides: StateFlow<Map<Long, VolumeChapterGroupDto>> = _volumeSectionOverrides.asStateFlow()

    private val _isLoadingMoreVolume = MutableStateFlow(false)

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    val comicIsIndexing: StateFlow<Boolean> = combine(
        directoryObserve.isIndexing,
        mangadexObserve.isIndexing,
    ) { directoryIndexing, remoteInfoIndexing ->
        directoryIndexing || remoteInfoIndexing
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val chapterIsIndexing: StateFlow<Boolean> = observeChaptersUseCase.isIndexing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val comicProgress: StateFlow<Int> = combine(
        directoryObserve.isIndexing,
        directoryObserve.progress,
        mangadexObserve.isIndexing,
        mangadexObserve.progress,
    ) { directoryBusy, directoryProg, remoteInfoBusy, remoteInfoProg ->
        when {
            directoryBusy && directoryProg != -1 -> directoryProg
            remoteInfoBusy && remoteInfoProg != -1 -> remoteInfoProg
            else -> -1
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    val chapterProgress: StateFlow<Int> = observeChaptersUseCase.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val comic: StateFlow<ComicDto?> = combine(
        selectedDirectoryId,
        selectedComicId,
        directoryObserve(),
        mangadexObserve(),
        selectedDirectoryId.flatMapLatest { id ->
            if (id == null) flowOf(null) else manageCategoriesUseCase.getCategoryByComicId(id)
        },
    ) { folderId, remoteInfoId, directories, remoteInfos, category ->
        if (folderId == null) return@combine null
        val directory = directories.find { it.id == folderId } ?: return@combine null

        var remote = remoteInfos.find { it.comicDirectoryFk == folderId }
        if (remote == null && remoteInfoId != null) {
            remote = remoteInfos.find { it.id == remoteInfoId }
        }
        if (remote == null) {
            val normalizedName = directory.name.normalizeKey()
            remote = remoteInfos.find { it.title.normalizeKey() == normalizedName }
        }

        ComicDto(directory = directory, remoteInfo = remote, category = category)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<ReadingHistoryDto?> = selectedDirectoryId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else observeComicHistoryUseCase.observeByComic(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val readChapters: StateFlow<List<String>> = selectedDirectoryId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else observeComicHistoryUseCase.observeReadChapters(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<ChapterDto?> = combine(
        selectedDirectoryId,
        selectedComicId,
        _chapterSortSettings,
        _currentPage,
        _selectedChapterPerPage,
        _volumeViewMode,
        _volumeSectionOverrides,
    ) { params ->
        val folderId = params[0] as Long?
        val remoteId = params[1] as Long?
        val sort = params[2] as ChapterSortPreferenceData
        val page = params[3] as Int
        val pageSizeType = params[4] as ChapterPageSizeType
        var viewMode = params[5] as VolumeViewType

        @Suppress("UNCHECKED_CAST")
        val volumeOverrides = params[6] as Map<Long, VolumeChapterGroupDto>

        if (folderId == null) null
        else ChapterParams(folderId, remoteId, sort, page, pageSizeType, viewMode, volumeOverrides)
    }.flatMapLatest { params ->
        if (params == null) return@flatMapLatest flowOf(null)

        observeChaptersUseCase.observeCombined(
            comicId = params.folderId,
            remoteId = params.remoteId,
            sort = params.sort,
            page = params.page,
            pageSize = params.pageSizeType.key.toInt(),
            viewMode = params.viewMode,
            volumeOverrides = params.volumeOverrides
        ).map { dto ->
            if (dto == null) return@map null
            
            val comicName = comic.value?.directory?.name ?: "Unknown"
            val viewMode = params.viewMode
            
            AcerolaLogger.i(TAG, "Comic Loaded: $comicName", LogSource.VIEWMODEL)
            AcerolaLogger.d(TAG, "Mode: ${dto.effectiveViewMode.name} | HasVolumeStructure: ${dto.hasVolumeStructure} | Sections: ${dto.archive.volumeSections.size}", LogSource.VIEWMODEL)
            
            if (dto.hasVolumeStructure) {
                dto.archive.volumeSections.forEach { section ->
                    AcerolaLogger.v(
                        TAG, 
                        "  > Volume: ${section.volume.name} | Chapters: ${section.items.size}/${section.totalChapters}", 
                        LogSource.VIEWMODEL
                    )
                }
            } else {
                AcerolaLogger.v(TAG, "  > Total Chapters: ${dto.archive.items.size}/${dto.archive.total}", LogSource.VIEWMODEL)
            }
            
            dto
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun init(
        folderId: Long,
        comicId: Long?
    ) {
        AcerolaLogger.audit(
            TAG, "Initializing ComicScreen", LogSource.VIEWMODEL, mapOf("folderId" to folderId.toString(), "comicId" to comicId.toString())
        )
        selectedDirectoryId.value = folderId
        selectedComicId.value = comicId
        _volumeSectionOverrides.value = emptyMap()
        _activeVolumeId.value = null

        viewModelScope.launch {
            ChapterPerPagePreference.chapterPerPageFlow(context).collect { size -> _selectedChapterPerPage.value = size }
        }
        viewModelScope.launch {
            ChapterSortPreference.sortFlow(context).collect { sort -> _chapterSortSettings.value = sort }
        }
        viewModelScope.launch {
            VolumeViewPreference.volumeViewFlow(context).collect { mode -> _volumeViewMode.value = mode }
        }
        viewModelScope.launch {
            comic.collect { comicDto ->
                val newRemoteId = comicDto?.remoteInfo?.id
                if (newRemoteId != null && newRemoteId != selectedComicId.value) {
                    AcerolaLogger.d(TAG, "Syncing remote ID: $newRemoteId", LogSource.VIEWMODEL)
                    selectedComicId.value = newRemoteId
                }
            }
        }
    }

    fun updateChapterPerPage(size: ChapterPageSizeType) {
        if (_selectedChapterPerPage.value == size) return
        AcerolaLogger.d(TAG, "Changing chapter page size to: ${size.name}", LogSource.VIEWMODEL)
        _selectedChapterPerPage.value = size
        _currentPage.value = 0
        _volumeSectionOverrides.value = emptyMap()
        viewModelScope.launch { ChapterPerPagePreference.saveChapterPerPage(context, size) }
    }

    fun updateChapterSort(sort: ChapterSortPreferenceData) {
        _activeVolumeId.value = null
        _chapterSortSettings.value = sort
        _currentPage.value = 0
        _volumeSectionOverrides.value = emptyMap()
        viewModelScope.launch { ChapterSortPreference.saveSort(context, sort) }
    }

    fun loadMoreChapters() {
        val current = _currentPage.value
        val total = chapters.value?.archive?.total ?: 0
        val pageSize = _selectedChapterPerPage.value.key.toInt()
        val totalPages = if (total == 0) 0 else kotlin.math.ceil(total.toDouble() / pageSize).toInt()

        if (current < totalPages - 1) {
            AcerolaLogger.d(TAG, "Loading next chapter page: ${current + 1}", LogSource.VIEWMODEL)
            _currentPage.value = current + 1
        }
    }

    fun updateVolumeViewMode(mode: VolumeViewType) {
        _volumeViewMode.value = mode
        _activeVolumeId.value = null
        _currentPage.value = 0
        _volumeSectionOverrides.value = emptyMap()
        viewModelScope.launch { VolumeViewPreference.saveVolumeView(context, mode) }
    }

    fun setActiveVolume(volumeId: Long?) {
        val previousId = _activeVolumeId.value
        if (previousId != null && previousId != volumeId) {
            _volumeSectionOverrides.value -= previousId
        }
        _activeVolumeId.value = volumeId
    }

    fun loadVolumeChaptersPage(
        volumeId: Long,
        page: Int
    ) {
        if (_isLoadingMoreVolume.value) return
        val comicId = selectedDirectoryId.value ?: return
        val currentSection = chapters.value?.archive?.volumeSections?.firstOrNull { it.volume.id == volumeId } ?: return
        val pageSize = _selectedChapterPerPage.value.key.toInt()

        viewModelScope.launch {
            _isLoadingMoreVolume.value = true
            try {
                val pageItems = directoryObserveVolumeChapters.loadVolumePage(
                    comicId = comicId,
                    volumeId = volumeId,
                    offset = page * pageSize,
                    pageSize = pageSize,
                    sortType = _chapterSortSettings.value.type.name,
                    isAscending = _chapterSortSettings.value.direction == SortDirection.ASCENDING,
                )
                
                val newItems = (currentSection.items + pageItems).distinctBy { it.id }
                
                _volumeSectionOverrides.value += (volumeId to currentSection.copy(
                    items = newItems,
                    loadedCount = newItems.size,
                    hasMore = newItems.size < currentSection.totalChapters,
                    currentPage = page,
                ))
            } finally {
                _isLoadingMoreVolume.value = false
            }
        }
    }

    fun loadPageAsync(page: Int) {
        if (page <= _currentPage.value) return
        AcerolaLogger.d(TAG, "Loading chapter list page: $page", LogSource.VIEWMODEL)
        _currentPage.value = page
    }

    fun toggleChapterReadStatus(chapterSort: String) {
        val comicId = selectedDirectoryId.value ?: return
        val isRead = readChapters.value.contains(chapterSort)
        val chapterId = chapters.value?.archive?.items?.find { it.chapterSort == chapterSort }?.id

        AcerolaLogger.audit(
            TAG, "Toggling chapter read status", LogSource.VIEWMODEL, mapOf("chapterSort" to chapterSort, "newStatus" to (!isRead).toString())
        )
        viewModelScope.launch { trackReadingProgressUseCase.toggleReadStatus(comicId, chapterSort, isRead, chapterId) }
    }

    private fun String.normalizeKey(): String = this.filter { it.isLetterOrDigit() }.lowercase()

    private data class ChapterParams(
        val folderId: Long,
        val remoteId: Long?,
        val sort: ChapterSortPreferenceData,
        val page: Int,
        val pageSizeType: ChapterPageSizeType,
        val viewMode: VolumeViewType,
        val volumeOverrides: Map<Long, VolumeChapterGroupDto>
    )

    companion object {

        private const val TAG = "ComicViewModel"
    }
}
