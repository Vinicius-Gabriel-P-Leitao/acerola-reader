package br.acerola.comic.module.comic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.comic.config.preference.ChapterPageSizeType
import br.acerola.comic.config.preference.ChapterPerPagePreference
import br.acerola.comic.config.preference.ChapterSortPreference
import br.acerola.comic.config.preference.ChapterSortPreferenceData
import br.acerola.comic.config.preference.ChapterSortType
import br.acerola.comic.config.preference.SortDirection
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.usecase.DirectoryCase
import br.acerola.comic.usecase.MangadexCase
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.usecase.chapter.ObserveVolumeChaptersUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.history.ObserveComicHistoryUseCase
import br.acerola.comic.usecase.history.TrackReadingProgressUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.max

@HiltViewModel
class ComicViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val observeComicHistoryUseCase: ObserveComicHistoryUseCase,
        private val trackReadingProgressUseCase: TrackReadingProgressUseCase,
        @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<ComicMetadataDto>,
        @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<ComicDirectoryDto>,
        @param:DirectoryCase private val directoryGetChapters: ObserveChaptersUseCase<ChapterArchivePageDto>,
        @param:DirectoryCase private val directoryObserveVolumeChapters: ObserveVolumeChaptersUseCase,
        @param:MangadexCase private val mangadexGetChapters: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>,
        private val manageCategoriesUseCase: ManageCategoriesUseCase,
    ) : ViewModel() {
        private val _selectedChapterPerPage = MutableStateFlow(value = ChapterPageSizeType.SHORT)
        val selectedChapterPerPage: StateFlow<ChapterPageSizeType> = _selectedChapterPerPage.asStateFlow()

        private val _chapterSortSettings = MutableStateFlow(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))
        val chapterSortSettings: StateFlow<ChapterSortPreferenceData> = _chapterSortSettings.asStateFlow()

        private val selectedDirectoryId = MutableStateFlow<Long?>(value = null)

        private val selectedMangaId = MutableStateFlow<Long?>(value = null)

        private val _currentPage = MutableStateFlow(0)
        val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

        private val _volumeSectionOverrides = MutableStateFlow<Map<Long, VolumeChapterGroupDto>>(emptyMap())

        private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
        val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

        val comicIsIndexing: StateFlow<Boolean> =
            combine(
                flow = directoryObserve.isIndexing,
                flow2 = mangadexObserve.isIndexing,
            ) { directoryIndexing, remoteInfoIndexing ->
                directoryIndexing || remoteInfoIndexing
            }.stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = false,
            )

        val chapterIsIndexing: StateFlow<Boolean> =
            combine(
                flow = directoryGetChapters.isIndexing,
                flow2 = mangadexGetChapters.isIndexing,
            ) { directoryIndexing, remoteInfoIndexing ->
                directoryIndexing || remoteInfoIndexing
            }.stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = false,
            )

        val comicProgress: StateFlow<Int> =
            combine(
                flow = directoryObserve.isIndexing,
                flow2 = directoryObserve.progress,
                flow3 = mangadexObserve.isIndexing,
                flow4 = mangadexObserve.progress,
            ) { directoryBusy, directoryProg, remoteInfoBusy, remoteInfoProg ->
                when {
                    directoryBusy && directoryProg != -1 -> directoryProg
                    remoteInfoBusy && remoteInfoProg != -1 -> remoteInfoProg
                    else -> -1
                }
            }.stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = -1,
            )

        val chapterProgress: StateFlow<Int> =
            combine(
                flow = directoryGetChapters.isIndexing,
                flow2 = directoryGetChapters.progress,
                flow3 = mangadexGetChapters.isIndexing,
                flow4 = mangadexGetChapters.progress,
            ) { directoryBusy, directoryProg, remoteInfoBusy, remoteInfoProg ->
                when {
                    directoryBusy && directoryProg != -1 -> directoryProg
                    remoteInfoBusy && remoteInfoProg != -1 -> remoteInfoProg
                    else -> -1
                }
            }.stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = -1,
            )

        @OptIn(ExperimentalCoroutinesApi::class)
        val comic: StateFlow<ComicDto?> =
            combine(
                selectedDirectoryId,
                selectedMangaId,
                directoryObserve(),
                mangadexObserve(),
                selectedDirectoryId.flatMapLatest { id ->
                    if (id == null) {
                        flowOf(null)
                    } else {
                        manageCategoriesUseCase.getCategoryByMangaId(id)
                    }
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
            }.stateIn(
                initialValue = null,
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            )

        @OptIn(ExperimentalCoroutinesApi::class)
        val history: StateFlow<ReadingHistoryDto?> =
            selectedDirectoryId
                .flatMapLatest { id ->
                    if (id == null) {
                        flowOf(null)
                    } else {
                        observeComicHistoryUseCase.observeByManga(id)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        @OptIn(ExperimentalCoroutinesApi::class)
        val readChapters: StateFlow<List<String>> =
            selectedDirectoryId
                .flatMapLatest { id ->
                    if (id == null) {
                        flowOf(emptyList())
                    } else {
                        observeComicHistoryUseCase.observeReadChapters(id)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        @OptIn(ExperimentalCoroutinesApi::class)
        val chapters: StateFlow<ChapterDto?> =
            combine(
                selectedDirectoryId,
                _chapterSortSettings,
                _currentPage,
                _selectedChapterPerPage,
                _volumeSectionOverrides,
            ) { folderId, sort, page, pageSizeType, volumeOverrides ->
                folderId to Quintuple(sort, page, pageSizeType, folderId, volumeOverrides)
            }.flatMapLatest { (folderId, params) ->
                if (folderId == null) return@flatMapLatest flowOf(null)
                val (sort, page, pageSizeType, _, volumeOverrides) = params

                val remoteFlow: Flow<ChapterRemoteInfoPageDto> =
                    comic.flatMapLatest {
                        val comicId = it?.remoteInfo?.id
                        if (comicId != null) {
                            mangadexGetChapters.observeByManga(comicId, sort.type.name, sort.direction == SortDirection.ASCENDING)
                        } else {
                            flowOf(ChapterRemoteInfoPageDto(emptyList(), 0, 0, 0))
                        }
                    }
                flow<ChapterDto?> {
                    val localFlow = directoryGetChapters.observeByManga(folderId, sort.type.name, sort.direction == SortDirection.ASCENDING)
                    val volumeSectionsFlow =
                        directoryObserveVolumeChapters.observeByComic(
                            comicId = folderId,
                            previewSize = VOLUME_PREVIEW_SIZE,
                            sortType = sort.type.name,
                            isAscending = sort.direction == SortDirection.ASCENDING,
                        )
                    val hasRootChaptersFlow = directoryObserveVolumeChapters.observeHasRootChapters(folderId)

                    emitAll(
                        combine(localFlow, volumeSectionsFlow, remoteFlow, hasRootChaptersFlow) { localAll, volumeSections, remoteAll, hasRootChapters ->
                            val shouldUseVolumeCards =
                                sort.type == ChapterSortType.NUMBER &&
                                    !hasRootChapters &&
                                    volumeSections.size > 0

                            if (shouldUseVolumeCards) {
                                val mergedSections =
                                    volumeSections.map { section ->
                                        volumeOverrides[section.volume.id]?.let { override ->
                                            section.copy(
                                                items = override.items,
                                                loadedCount = override.loadedCount,
                                                hasMore = override.hasMore,
                                            )
                                        } ?: section
                                    }

                                val visibleItems = mergedSections.flatMap { it.items }
                                val remoteMap = remoteAll.items.associateBy { it.chapter.normalizeChapter() }
                                val filteredRemoteItems =
                                    visibleItems.mapNotNull { local ->
                                        remoteMap[local.chapterSort.normalizeChapter()]
                                    }

                                ChapterDto(
                                    archive =
                                        ChapterArchivePageDto(
                                            items = visibleItems,
                                            volumes = mergedSections.map { it.volume },
                                            volumeSections = mergedSections,
                                            pageSize = VOLUME_PREVIEW_SIZE,
                                            total = mergedSections.sumOf { it.totalChapters },
                                            page = 0,
                                        ),
                                    remoteInfo = ChapterRemoteInfoPageDto(filteredRemoteItems, VOLUME_PREVIEW_SIZE, 0, visibleItems.size),
                                    showVolumeHeaders = true,
                                )
                            } else {
                                val items = localAll.items
                                if (items.isEmpty()) return@combine ChapterDto(localAll, remoteAll)

                                val total = items.size
                                val pageSize = pageSizeType.key.toInt()
                                val totalPages = ceil(total.toDouble() / pageSize).toInt()
                                val safePage = page.coerceIn(0, max(0, totalPages - 1))

                                val start = safePage * pageSize
                                val end = (start + pageSize).coerceIn(0, total)

                                val pagedLocalItems = if (start < total) items.subList(start, end) else emptyList()
                                val remoteMap = remoteAll.items.associateBy { it.chapter.normalizeChapter() }
                                val filteredRemoteItems =
                                    pagedLocalItems.mapNotNull { local ->
                                        remoteMap[local.chapterSort.normalizeChapter()]
                                    }

                                ChapterDto(
                                    archive =
                                        ChapterArchivePageDto(
                                            items = pagedLocalItems,
                                            volumes = localAll.volumes,
                                            pageSize = pageSize,
                                            total = total,
                                            page = safePage,
                                        ),
                                    remoteInfo = ChapterRemoteInfoPageDto(filteredRemoteItems, pageSize, safePage, total),
                                    showVolumeHeaders = false,
                                )
                            }
                        },
                    )
                }
            }.stateIn(
                initialValue = null,
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            )

// Add this helper class at the end of the file or use a Pair/Triple nesting
        data class Quintuple<A, B, C, D, E>(
            val first: A,
            val second: B,
            val third: C,
            val fourth: D,
            val fifth: E,
        )

        fun init(
            folderId: Long,
            comicId: Long?,
        ) {
            AcerolaLogger.audit(
                TAG,
                "Initializing MangaScreen",
                LogSource.VIEWMODEL,
                mapOf("folderId" to folderId.toString(), "comicId" to comicId.toString()),
            )
            selectedDirectoryId.value = folderId
            selectedMangaId.value = comicId
            _volumeSectionOverrides.value = emptyMap()

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
                comic.collect { comicDto ->
                    val newRemoteId = comicDto?.remoteInfo?.id
                    if (newRemoteId != null && newRemoteId != selectedMangaId.value) {
                        AcerolaLogger.d(TAG, "Syncing remote ID: $newRemoteId", LogSource.VIEWMODEL)
                        selectedMangaId.value = newRemoteId
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
            _volumeSectionOverrides.value = emptyMap()
            viewModelScope.launch {
                ChapterSortPreference.saveSort(context, sort)
            }
        }

        fun loadMoreVolumeChapters(volumeId: Long) {
            val comicId = selectedDirectoryId.value ?: return
            val currentSection = chapters.value?.archive?.volumeSections?.firstOrNull { it.volume.id == volumeId } ?: return
            if (!currentSection.hasMore) return

            viewModelScope.launch {
                val nextItems =
                    directoryObserveVolumeChapters.loadVolumePage(
                        comicId = comicId,
                        volumeId = volumeId,
                        offset = currentSection.loadedCount,
                        pageSize = _selectedChapterPerPage.value.key.toInt(),
                        sortType = _chapterSortSettings.value.type.name,
                        isAscending = _chapterSortSettings.value.direction == SortDirection.ASCENDING,
                    )

                val mergedItems = (currentSection.items + nextItems).distinctBy { it.id }
                _volumeSectionOverrides.value =
                    _volumeSectionOverrides.value + (
                        volumeId to
                            currentSection.copy(
                                items = mergedItems,
                                loadedCount = mergedItems.size,
                                hasMore = mergedItems.size < currentSection.totalChapters,
                            )
                    )
            }
        }

        fun loadPageAsync(page: Int) {
            AcerolaLogger.d(TAG, "Loading chapter list page: $page", LogSource.VIEWMODEL)
            _currentPage.value = page
        }

        fun toggleChapterReadStatus(
            chapterSort: String,
        ) {
            val comicId = selectedDirectoryId.value ?: return
            val isRead = readChapters.value.contains(chapterSort)
            val chapterId = chapters.value?.archive?.items?.find { it.chapterSort == chapterSort }?.id

            AcerolaLogger.audit(
                TAG,
                "Toggling chapter read status",
                LogSource.VIEWMODEL,
                mapOf("chapterSort" to chapterSort, "newStatus" to (!isRead).toString()),
            )

            viewModelScope.launch {
                trackReadingProgressUseCase.toggleReadStatus(comicId, chapterSort, isRead, chapterId)
            }
        }

        private fun String.normalizeKey(): String = this.filter { it.isLetterOrDigit() }.lowercase()

        companion object {
            private const val TAG = "MangaViewModel"
            private const val VOLUME_PREVIEW_SIZE = 5
        }
    }
