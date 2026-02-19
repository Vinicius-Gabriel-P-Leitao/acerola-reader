package br.acerola.manga.module.manga

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
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
import br.acerola.manga.usecase.chapter.GetChaptersUseCase
import br.acerola.manga.usecase.di.DirectoryCase
import br.acerola.manga.usecase.di.MangadexCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.util.normalizeChapter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Criar reescan de manga, ele pega o nome do ID da pasta e busca e após isso tentar fazer
//  scan só dele.
@HiltViewModel
class MangaViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>,
    @param:DirectoryCase private val directoryGetChapters: GetChaptersUseCase<ChapterArchivePageDto>,
    @param:MangadexCase private val mangadexGetChapters: GetChaptersUseCase<ChapterRemoteInfoPageDto>,
    private val syncMangaMetadataUseCase: br.acerola.manga.usecase.metadata.SyncMangaMetadataUseCase,
    private val fileSystemAccessManager: br.acerola.manga.config.permission.FileSystemAccessManager
) : ViewModel() {

    private val _selectedChapterPerPage = MutableStateFlow(value = ChapterPageSizeType.SHORT)
    val selectedChapterPerPage: StateFlow<ChapterPageSizeType> = _selectedChapterPerPage.asStateFlow()

    private val _selectedDirectoryId = MutableStateFlow<Long?>(value = null)
    val selectedDirectoryId: StateFlow<Long?> = _selectedDirectoryId.asStateFlow()

    private val _selectedMangaId = MutableStateFlow<Long?>(value = null)
    val selectedMangaId: StateFlow<Long?> = _selectedMangaId.asStateFlow()

    private val _chapter = MutableStateFlow<ChapterDto?>(value = null)
    val chapters: StateFlow<ChapterDto?> = _chapter.asStateFlow()

    private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
    val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

    private val _isMetadataIndexing = MutableStateFlow(value = false)
    val isMetadataIndexing: StateFlow<Boolean> = _isMetadataIndexing.asStateFlow()

    // TODO: Transformar em config do DataStore
    private var currentPage = 0
    private var pageSize = 20
    private var total = 0

    val mangaIsIndexing: StateFlow<Boolean> = combine(
        flow = directoryObserve.isIndexing, 
        flow2 = mangadexObserve.isIndexing,
        flow3 = _isMetadataIndexing
    ) { directoryIndexing, remoteInfoIndexing, metadataIndexing ->
        directoryIndexing || remoteInfoIndexing || metadataIndexing
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = false
    )

    fun syncFromMangadex(folderId: Long, title: String) {
        viewModelScope.launch {
            _isMetadataIndexing.value = true
            val rootUri = getRootUri() ?: return@launch
            val mangaId = _selectedMangaId.value ?: -1L
            syncMangaMetadataUseCase.syncFromMangadex(mangaId, folderId, title, rootUri).onLeft {
                _uiEvents.send(element = it)
            }
            _isMetadataIndexing.value = false
        }
    }

    fun syncFromComicInfo(folderId: Long, title: String, folderUri: String) {
        viewModelScope.launch {
            _isMetadataIndexing.value = true
            val rootUri = getRootUri() ?: return@launch
            val mangaId = _selectedMangaId.value ?: -1L
            syncMangaMetadataUseCase.syncFromComicInfo(mangaId, folderId, title, folderUri.toUri(), rootUri).onLeft {
                _uiEvents.send(element = it)
            }
            _isMetadataIndexing.value = false
        }
    }

    private suspend fun getRootUri(): Uri? {
        fileSystemAccessManager.loadFolderUri()
        return fileSystemAccessManager.folderUri
    }

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

        var remote = if (remoteInfoId != null) {
            remoteInfos.find { it.id == remoteInfoId }
        } else null

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

    fun init(folderId: Long, mangaId: Long?) {
        _selectedDirectoryId.value = folderId
        _selectedMangaId.value = mangaId

        // Observe preference changes and trigger load
        viewModelScope.launch {
            ChapterPerPagePreference.chapterPerPageFlow(context).collect { size ->
                _selectedChapterPerPage.value = size
                pageSize = size.key.toInt()
                loadPage(page = currentPage)
            }
        }

        // NOTE: Atualiza a pagina.
        viewModelScope.launch {
            chapterIsIndexing.collect { indexing ->
                if (!indexing) {
                    loadPage(page = currentPage)
                }
            }
        }

        observeChapterPerPage()
    }

    fun updateChapterPerPage(size: ChapterPageSizeType) {
        if (_selectedChapterPerPage.value == size) return
        _selectedChapterPerPage.value = size
        viewModelScope.launch {
            ChapterPerPagePreference.saveChapterPerPage(context, size)
        }
    }

    private fun observeChapterPerPage() {
        viewModelScope.launch {
            ChapterPerPagePreference.chapterPerPageFlow(context).collect { size ->
                if (_selectedChapterPerPage.value != size) {
                    _selectedChapterPerPage.value = size
                }
            }

            chapterIsIndexing.collect { indexing ->
                if (!indexing) {
                    loadPage(page = currentPage)
                }
            }
        }
    }

    fun loadPageAsync(page: Int) {
        viewModelScope.launch {
            loadPage(page)
        }
    }

    private suspend fun loadPage(page: Int) {
        val folderId = _selectedDirectoryId.value ?: return
        val mangaId = _selectedMangaId.value

        currentPage = page

        if (total == 0) {
            total = directoryGetChapters.loadPage(
                mangaId = folderId, total = 0, page = 0, pageSize = pageSize
            ).total
        }

        val localPage = directoryGetChapters.loadPage(
            mangaId = folderId, total = total, page = page, pageSize = pageSize
        )

        val chapterSorts = localPage.items.map {
            it.chapterSort
        }

        // NOTE: Para assimiliar com o remote.
        val searchChapters = chapterSorts.flatMap {
            val parts = it.split(".")

            if (parts.size == 2 && parts[1].length == 1) {
                listOf(it, "${parts[0]}.0${parts[1]}")
            } else {
                listOf(it)
            }
        }.distinct()

        val remotePage = mangaId?.let {
            mangadexGetChapters.observeSpecific(
                mangaId = it, chapters = searchChapters
            ).first()
        } ?: ChapterRemoteInfoPageDto(
            items = emptyList(), pageSize = pageSize, page = page, total = total
        )

        val remoteMap = remotePage.items.associateBy {
            it.chapter.normalizeChapter()
        }

        val filteredRemoteItems = localPage.items.mapNotNull {
            remoteMap[it.chapterSort.normalizeChapter()]
        }

        _chapter.value = ChapterDto(
            archive = localPage, remoteInfo = remotePage.copy(items = filteredRemoteItems)
        )
    }

    private fun String.normalizeKey(): String {
        return this.filter { it.isLetterOrDigit() }.lowercase()
    }
}
