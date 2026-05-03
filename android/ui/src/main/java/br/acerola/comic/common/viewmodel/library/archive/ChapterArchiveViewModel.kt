package br.acerola.comic.common.viewmodel.library.archive
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.comic.config.permission.FileSystemAccessManager
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.usecase.DirectoryCase
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChapterArchiveViewModel
    @Inject
    constructor(
        private val scheduler: br.acerola.comic.sync.LibrarySyncScheduler,
        private val statusRepository: br.acerola.comic.sync.LibrarySyncStatusRepository,
        private val manager: FileSystemAccessManager,
        @param:DirectoryCase private val observeChaptersUseCase: ObserveChaptersUseCase<ChapterPageDto>,
    ) : ViewModel() {
        val isIndexing: StateFlow<Boolean> = statusRepository.isIndexing
        val progress: StateFlow<Int> = statusRepository.progress

        private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
        val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

        private val chapterPage = MutableStateFlow<ChapterPageDto?>(value = null)

        private val selectedDirectoryId = MutableStateFlow<Long?>(value = null)

        private var currentPage = 0
        private val pageSize = 20
        private var total = 0

        fun init(
            directoryId: Long,
            firstPage: ChapterPageDto,
        ) {
            AcerolaLogger.d(TAG, "Initializing with directoryId: $directoryId", LogSource.VIEWMODEL)
            selectedDirectoryId.value = directoryId
            chapterPage.value = firstPage
            currentPage = firstPage.page
            total = firstPage.total
        }

        fun loadPage(page: Int) {
            AcerolaLogger.d(TAG, "Loading local chapter page: $page", LogSource.VIEWMODEL)
            viewModelScope.launch {
                chapterPage.value = null

                val result: ChapterPageDto =
                    observeChaptersUseCase.loadPage(
                        comicId = selectedDirectoryId.value!!,
                        pageSize = pageSize,
                        total = total,
                        page = page,
                    )

                chapterPage.value = result
            }
        }

        fun syncChaptersByMangaDirectory(folderId: Long) {
            AcerolaLogger.audit(
                TAG,
                "User requested local chapter rescan",
                LogSource.VIEWMODEL,
                mapOf("folderId" to folderId.toString()),
            )
            viewModelScope.launch {
                manager.loadFolderUri()
                scheduler.enqueueSpecific(folderId, manager.folderUri)
            }
        }

        companion object {
            private const val TAG = "ChapterArchiveViewModel"
        }
    }
