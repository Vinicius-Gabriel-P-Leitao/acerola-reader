package br.acerola.comic.common.viewmodel.library.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.error.UserMessage
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.usecase.MangadexCase
import br.acerola.comic.usecase.chapter.ObserveChaptersUseCase
import br.acerola.comic.util.sort.normalizeSort
import br.acerola.comic.worker.sync.MetadataSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChapterMetadataViewModel
    @Inject
    constructor(
        private val workManager: WorkManager,
        @param:MangadexCase private val getMangadexChaptersUseCase: ObserveChaptersUseCase<ChapterRemoteInfoPageDto>,
    ) : ViewModel() {
        val isIndexing: StateFlow<Boolean> =
            getMangadexChaptersUseCase.isIndexing
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val progress: StateFlow<Int> =
            getMangadexChaptersUseCase.progress
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

        private val _uiEvents = Channel<UserMessage>(capacity = Channel.BUFFERED)
        val uiEvents: Flow<UserMessage> = _uiEvents.receiveAsFlow()

        private val chapterPage = MutableStateFlow<ChapterRemoteInfoPageDto?>(value = null)

        private val selectedMangaId = MutableStateFlow<Long?>(value = null)

        private var currentPage = 0
        private val pageSize = 20
        private var total = 0

        fun init(
            comicId: Long,
            firstPage: ChapterRemoteInfoPageDto,
        ) {
            AcerolaLogger.d(TAG, "Initializing with comicId: $comicId", LogSource.VIEWMODEL)
            selectedMangaId.value = comicId
            total = firstPage.total
            currentPage = firstPage.page
            chapterPage.value = firstPage
        }

        fun loadPage(page: Int) {
            AcerolaLogger.d(TAG, "Loading metadata page: $page", LogSource.VIEWMODEL)
            viewModelScope.launch {
                chapterPage.value = null

                val result: ChapterRemoteInfoPageDto =
                    getMangadexChaptersUseCase.loadPage(
                        comicId = selectedMangaId.value!!,
                        pageSize = pageSize,
                        total = total,
                        page = page,
                    )

                val sortedItems: List<ChapterFeedDto> =
                    result.items.sortedBy {
                        it.chapter.normalizeSort().toFloatOrNull() ?: 0f
                    }

                chapterPage.value = result.copy(items = sortedItems)
            }
        }

        fun syncChaptersByMangadex(comicId: Long) {
            AcerolaLogger.audit(
                TAG,
                "User requested chapter sync from MangaDex",
                LogSource.VIEWMODEL,
                mapOf("comicId" to comicId.toString()),
            )
            enqueueMetadataSync(MetadataSyncWorker.SOURCE_MANGADEX, comicId)
        }

        fun syncChaptersByComicInfo(folderId: Long) {
            AcerolaLogger.audit(
                TAG,
                "User requested chapter sync from ComicInfo.xml",
                LogSource.VIEWMODEL,
                mapOf("folderId" to folderId.toString()),
            )
            enqueueMetadataSync(MetadataSyncWorker.SOURCE_COMICINFO, folderId)
        }

        private fun enqueueMetadataSync(
            source: String,
            directoryId: Long,
        ) {
            AcerolaLogger.d(
                TAG,
                "Enqueuing metadata sync worker from ChapterViewModel: source=$source, directoryId=$directoryId",
                LogSource.VIEWMODEL,
            )
            viewModelScope.launch {
                val syncRequest =
                    OneTimeWorkRequestBuilder<MetadataSyncWorker>()
                        .setInputData(
                            workDataOf(
                                MetadataSyncWorker.KEY_SYNC_SOURCE to source,
                                MetadataSyncWorker.KEY_DIRECTORY_ID to directoryId,
                                MetadataSyncWorker.KEY_SYNC_TYPE to MetadataSyncWorker.SYNC_TYPE_RESCAN,
                            ),
                        ).addTag("metadata_sync")
                        .build()

                workManager.enqueueUniqueWork(
                    "metadata_sync_$directoryId",
                    ExistingWorkPolicy.KEEP,
                    syncRequest,
                )
            }
        }

        companion object {
            private const val TAG = "ChapterRemoteInfoViewModel"
        }
    }
