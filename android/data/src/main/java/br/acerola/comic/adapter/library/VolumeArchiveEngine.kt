package br.acerola.comic.adapter.library

import br.acerola.comic.adapter.contract.gateway.VolumeChapterGateway
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.VolumeArchiveDao
import br.acerola.comic.local.translator.ui.toGroupDto
import br.acerola.comic.local.translator.ui.toViewDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeArchiveEngine
    @Inject
    constructor(
        private val volumeArchiveDao: VolumeArchiveDao,
        private val chapterArchiveDao: ChapterArchiveDao,
    ) : VolumeChapterGateway {
        override fun observeVolumeGroups(
            comicId: Long,
            previewSize: Int,
            sortType: String,
            isAscending: Boolean,
        ): StateFlow<List<VolumeChapterGroupDto>> =
            volumeArchiveDao
                .getVolumeChapterCountsByDirectoryId(comicId)
                .map { summaries ->
                    val sortedSummaries = if (isAscending) summaries else summaries.reversed()

                    sortedSummaries.map { summary ->
                        val previewItems =
                            chapterArchiveDao
                                .getChaptersByVolumePaged(
                                    comicId = comicId,
                                    volumeId = summary.id,
                                    pageSize = previewSize,
                                    offset = 0,
                                ).let { joins ->
                                    val base = joins.map { it.toViewDto() }

                                    if (sortType == "LAST_UPDATE") {
                                        val ordered = base.sortedBy { it.lastModified }
                                        if (isAscending) ordered else ordered.reversed()
                                    } else {
                                        if (isAscending) base else base.reversed()
                                    }
                                }

                        summary.toGroupDto(items = previewItems)
                    }
                }.stateIn(
                    started = SharingStarted.Lazily,
                    scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
                    initialValue = emptyList(),
                )

        override suspend fun getVolumeChapterPage(
            comicId: Long,
            volumeId: Long,
            offset: Int,
            pageSize: Int,
            sortType: String,
            isAscending: Boolean,
        ): List<ChapterFileDto> =
            chapterArchiveDao
                .getChaptersByVolumePaged(
                    comicId = comicId,
                    volumeId = volumeId,
                    pageSize = pageSize,
                    offset = offset,
                ).let { joins ->
                    val base = joins.map { it.toViewDto() }

                    if (sortType == "LAST_UPDATE") {
                        val ordered = base.sortedBy { it.lastModified }
                        if (isAscending) ordered else ordered.reversed()
                    } else {
                        if (isAscending) base else base.reversed()
                    }
                }

        override fun observeHasRootChapters(comicId: Long): Flow<Boolean> =
            chapterArchiveDao.observeRootChaptersCountByDirectoryId(comicId).map { it > 0 }

        companion object {
            private const val TAG = "VolumeArchiveEngine"
        }
    }
