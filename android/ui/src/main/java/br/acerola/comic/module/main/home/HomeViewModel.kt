package br.acerola.comic.module.main.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.acerola.comic.config.preference.ComicSortPreference
import br.acerola.comic.config.preference.ComicSortType
import br.acerola.comic.config.preference.HomeFilterPreference
import br.acerola.comic.config.preference.HomeLayoutPreference
import br.acerola.comic.config.preference.HomeLayoutType
import br.acerola.comic.config.preference.HomeSortPreference
import br.acerola.comic.config.preference.SortDirection
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.module.main.home.state.FilterSettings
import br.acerola.comic.usecase.DirectoryCase
import br.acerola.comic.usecase.MangadexCase
import br.acerola.comic.usecase.chapter.GetChapterCountUseCase
import br.acerola.comic.usecase.comic.DeleteComicUseCase
import br.acerola.comic.usecase.comic.HideComicUseCase
import br.acerola.comic.usecase.comic.ObserveLibraryUseCase
import br.acerola.comic.usecase.history.ObserveHistoryUseCase
import br.acerola.comic.usecase.metadata.ManageCategoriesUseCase
import br.acerola.comic.worker.WorkerContract
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class HomeCombinedArgs(
    val mangaDirectories: List<ComicDirectoryDto>,
    val remoteMangaInfo: List<ComicMetadataDto>,
    val historyList: List<ReadingHistoryDto>,
    val categoryMap: Map<Long, CategoryDto>,
    val chapterCounts: Map<Long, Int>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    workManager: WorkManager,
    observeHistoryUseCase: ObserveHistoryUseCase,
    getChapterCountUseCase: GetChapterCountUseCase,
    private val manageCategoriesUseCase: ManageCategoriesUseCase,
    private val hideComicUseCase: HideComicUseCase,
    private val deleteComicUseCase: DeleteComicUseCase,
    @param:ApplicationContext private val context: Context,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<ComicMetadataDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<ComicDirectoryDto>,
) : ViewModel() {

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _selectedHomeLayout = MutableStateFlow(value = HomeLayoutType.LIST)
    val selectedHomeLayout: StateFlow<HomeLayoutType> = _selectedHomeLayout.asStateFlow()

    private val _sortSettings = MutableStateFlow(HomeSortPreference(ComicSortType.TITLE, SortDirection.ASCENDING))
    val sortSettings: StateFlow<HomeSortPreference> = _sortSettings.asStateFlow()

    private val _filterSettings = MutableStateFlow(FilterSettings())
    val filterSettings: StateFlow<FilterSettings> = _filterSettings.asStateFlow()

    val allCategories: StateFlow<List<CategoryDto>> = manageCategoriesUseCase.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), emptyList())

    val isIndexing: StateFlow<Boolean> = workManager.getWorkInfosByTagFlow(WorkerContract.TAG_LIBRARY_SYNC)
        .map { workInfos ->
            workInfos.any { !it.state.isFinished }
        }.stateIn(
            viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false
        )

    val progress: StateFlow<Int> = workManager.getWorkInfosByTagFlow(WorkerContract.TAG_LIBRARY_SYNC)
        .map { workInfos ->
            val activeWorker = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            activeWorker?.progress?.getInt(WorkerContract.KEY_PROGRESS, -1) ?: -1
        }.stateIn(
            viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = -1
        )

    val mangas: StateFlow<List<Triple<ComicDto, ReadingHistoryDto?, Int>>?> = combine(
        combine(
            directoryObserve(),
            mangadexObserve(),
            observeHistoryUseCase.invokeRecent(),
            manageCategoriesUseCase.getAllMangaCategories(),
            getChapterCountUseCase()
        ) { directories, remote, history, categories, counts ->
            HomeCombinedArgs(directories, remote, history, categories, counts)
        },
        _sortSettings,
        _filterSettings
    ) { args, sort, filter ->
        val remoteInfoMap = args.remoteMangaInfo.filter { it.mangaDirectoryFk != null }
            .associateBy { it.mangaDirectoryFk!! }

        val historyMap = args.historyList.associateBy { it.mangaDirectoryId }

        val list = args.mangaDirectories
            .filter { directory ->
                val matchesHidden = filter.showHidden || !directory.hidden
                val matchesCategory = filter.bookmarkCategoryId == null || args.categoryMap[directory.id]?.id == filter.bookmarkCategoryId
                
                val source = remoteInfoMap[directory.id]?.syncSource?.displayName
                val matchesSource = when (filter.metadataSource) {
                    null -> true
                    "NONE" -> source == null
                    else -> source == filter.metadataSource
                }

                matchesHidden && matchesCategory && matchesSource
            }
            .map { directory ->
                val manga = ComicDto(
                    directory = directory,
                    remoteInfo = remoteInfoMap[directory.id],
                    category = args.categoryMap[directory.id]
                )
                Triple(manga, historyMap[directory.id], args.chapterCounts[directory.id] ?: 0)
            }

        // Apply Sorting
        val sortedList = when (sort.type) {
            ComicSortType.TITLE -> list.sortedBy { it.first.remoteInfo?.title ?: it.first.directory.name }
            ComicSortType.CHAPTER_COUNT -> list.sortedBy { it.third }
            ComicSortType.LAST_UPDATE -> list.sortedBy { it.first.directory.lastModified }
        }

        val finalList = if (sort.direction == SortDirection.DESCENDING) sortedList.reversed() else sortedList

        AcerolaLogger.d(TAG, "Library loaded: ${finalList.size} mangas found", LogSource.VIEWMODEL)
        finalList
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = null
    )

    init {
        observeHomeLayout()
        observeSortSettings()
        observeFilterSettings()
    }

    fun hideManga(mangaId: Long) {
        viewModelScope.launch {
            hideComicUseCase(mangaId).onLeft { error ->
                _uiEvents.send(error)
            }
        }
    }

    fun deleteComic(mangaId: Long) {
        viewModelScope.launch {
            deleteComicUseCase(mangaId).onLeft { error ->
                _uiEvents.send(error)
            }
        }
    }

    fun setMangaCategory(mangaId: Long, categoryId: Long?) {
        viewModelScope.launch {
            manageCategoriesUseCase.updateMangaCategory(mangaId, categoryId)
        }
    }

    fun updateHomeLayout(layout: HomeLayoutType) {
        if (_selectedHomeLayout.value == layout) return
        _selectedHomeLayout.value = layout

        AcerolaLogger.audit(TAG, "User changed home layout to ${layout.name}", LogSource.VIEWMODEL)

        viewModelScope.launch {
            HomeLayoutPreference.saveLayout(context, layout)
        }
    }

    fun updateSortSettings(sort: HomeSortPreference) {
        _sortSettings.value = sort
        viewModelScope.launch {
            ComicSortPreference.saveSort(context, sort)
        }
    }

    fun updateFilterSettings(filter: FilterSettings) {
        val oldShowHidden = _filterSettings.value.showHidden
        _filterSettings.value = filter
        
        if (oldShowHidden != filter.showHidden) {
            viewModelScope.launch {
                HomeFilterPreference.saveShowHidden(context, filter.showHidden)
            }
        }
    }

    private fun observeHomeLayout() {
        viewModelScope.launch {
            HomeLayoutPreference.layoutFlow(context).collect { layout ->
                if (_selectedHomeLayout.value != layout) {
                    _selectedHomeLayout.value = layout
                }
            }
        }
    }

    private fun observeSortSettings() {
        viewModelScope.launch {
            ComicSortPreference.sortFlow(context).collect { sort ->
                _sortSettings.value = sort
            }
        }
    }

    private fun observeFilterSettings() {
        viewModelScope.launch {
            HomeFilterPreference.showHiddenFlow(context).collect { showHidden ->
                if (_filterSettings.value.showHidden != showHidden) {
                    _filterSettings.value = _filterSettings.value.copy(showHidden = showHidden)
                }
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
