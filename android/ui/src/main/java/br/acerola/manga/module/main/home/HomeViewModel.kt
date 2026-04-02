package br.acerola.manga.module.main.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import br.acerola.manga.config.preference.HomeFilterPreference
import br.acerola.manga.config.preference.HomeLayoutPreference
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.config.preference.HomeSortPreference
import br.acerola.manga.config.preference.MangaSortPreference
import br.acerola.manga.config.preference.MangaSortType
import br.acerola.manga.config.preference.SortDirection
import br.acerola.manga.core.usecase.DirectoryCase
import br.acerola.manga.core.usecase.MangadexCase
import br.acerola.manga.core.usecase.chapter.GetChapterCountUseCase
import br.acerola.manga.core.usecase.history.ObserveHistoryUseCase
import br.acerola.manga.core.usecase.manga.DeleteMangaUseCase
import br.acerola.manga.core.usecase.manga.HideMangaUseCase
import br.acerola.manga.core.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.core.usecase.metadata.ManageCategoriesUseCase
import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.module.main.home.state.FilterSettings
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
    val mangaDirectories: List<MangaDirectoryDto>,
    val remoteMangaInfo: List<MangaMetadataDto>,
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
    private val hideMangaUseCase: HideMangaUseCase,
    private val deleteMangaUseCase: DeleteMangaUseCase,
    @param:ApplicationContext private val context: Context,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<MangaMetadataDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>,
) : ViewModel() {

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _selectedHomeLayout = MutableStateFlow(value = HomeLayoutType.LIST)
    val selectedHomeLayout: StateFlow<HomeLayoutType> = _selectedHomeLayout.asStateFlow()

    private val _sortSettings = MutableStateFlow(HomeSortPreference(MangaSortType.TITLE, SortDirection.ASCENDING))
    val sortSettings: StateFlow<HomeSortPreference> = _sortSettings.asStateFlow()

    private val _filterSettings = MutableStateFlow(FilterSettings())
    val filterSettings: StateFlow<FilterSettings> = _filterSettings.asStateFlow()

    val allCategories: StateFlow<List<CategoryDto>> = manageCategoriesUseCase.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), emptyList())

    val isIndexing: StateFlow<Boolean> = workManager.getWorkInfosByTagFlow("library_sync")
        .map { workInfos ->
            workInfos.any { !it.state.isFinished }
        }.stateIn(
            viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false
        )

    val progress: StateFlow<Int> = workManager.getWorkInfosByTagFlow("library_sync")
        .map { workInfos ->
            val activeWorker = workInfos.firstOrNull { !it.state.isFinished }
            activeWorker?.progress?.getInt("progress", -1) ?: -1
        }.stateIn(
            viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = -1
        )

    val mangas: StateFlow<List<Triple<MangaDto, ReadingHistoryDto?, Int>>> = combine(
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
                val manga = MangaDto(
                    directory = directory,
                    remoteInfo = remoteInfoMap[directory.id],
                    category = args.categoryMap[directory.id]
                )
                Triple(manga, historyMap[directory.id], args.chapterCounts[directory.id] ?: 0)
            }

        // Apply Sorting
        val sortedList = when (sort.type) {
            MangaSortType.TITLE -> list.sortedBy { it.first.remoteInfo?.title ?: it.first.directory.name }
            MangaSortType.CHAPTER_COUNT -> list.sortedBy { it.third }
            MangaSortType.LAST_UPDATE -> list.sortedBy { it.first.directory.lastModified }
        }

        val finalList = if (sort.direction == SortDirection.DESCENDING) sortedList.reversed() else sortedList

        AcerolaLogger.d(TAG, "Library loaded: ${finalList.size} mangas found", LogSource.VIEWMODEL)
        finalList
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = emptyList()
    )

    init {
        observeHomeLayout()
        observeSortSettings()
        observeFilterSettings()
    }

    fun hideManga(mangaId: Long) {
        viewModelScope.launch {
            hideMangaUseCase(mangaId).onLeft { error ->
                _uiEvents.send(error)
            }
        }
    }

    fun deleteManga(mangaId: Long) {
        viewModelScope.launch {
            deleteMangaUseCase(mangaId).onLeft { error ->
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
            MangaSortPreference.saveSort(context, sort)
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
            MangaSortPreference.sortFlow(context).collect { sort ->
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
