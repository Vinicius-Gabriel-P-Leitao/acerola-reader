package br.acerola.comic.adapter.library.engine

import br.acerola.comic.adapter.contract.gateway.VolumeGateway
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.VolumeArchiveDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.VolumeArchiveDao
import br.acerola.comic.local.entity.relation.VolumeChapterCount
import br.acerola.comic.local.translator.ui.toViewDto
import br.acerola.comic.local.translator.ui.toVolumeGroupDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeArchiveEngine
    @Inject
    constructor(
        private val volumeArchiveDao: VolumeArchiveDao,
        private val chapterArchiveDao: ChapterArchiveDao,
    ) : VolumeGateway {
        override fun observeVolumeGroups(
            comicId: Long,
            previewSize: Int,
            sortType: String,
            isAscending: Boolean,
        ): Flow<List<VolumeChapterGroupDto>> =
            volumeArchiveDao
                .getVolumeChapterCountsByDirectoryId(comicId)
                .map<List<VolumeChapterCount>, List<VolumeChapterGroupDto>> { summaries ->
                    val sortedSummaries = if (isAscending) summaries else summaries.reversed()

                    sortedSummaries.map { summary ->
                        val previewItems =
                            if (isAscending) {
                                chapterArchiveDao.getChaptersByVolumePaged(
                                    comicId = comicId,
                                    volumeId = summary.id,
                                    pageSize = previewSize,
                                    offset = 0,
                                )
                            } else {
                                chapterArchiveDao.getChaptersByVolumePagedDesc(
                                    comicId = comicId,
                                    volumeId = summary.id,
                                    pageSize = previewSize,
                                    offset = 0,
                                )
                            }.let { joins ->
                                joins.map { it.toViewDto() }
                            }

                        summary.toVolumeGroupDto(items = previewItems)
                    }
                }.onStart {
                    emit(
                        listOf(
                            VolumeChapterGroupDto(
                                volume = VolumeArchiveDto(-1, "", "", false),
                                items = emptyList(),
                                totalChapters = -1,
                                loadedCount = 0,
                                hasMore = false,
                            ),
                        ),
                    )
                }

        override suspend fun getVolumeChapterPage(
            comicId: Long,
            volumeId: Long,
            offset: Int,
            pageSize: Int,
            sortType: String,
            isAscending: Boolean,
        ): List<ChapterFileDto> {
            val joins =
                if (isAscending) {
                    chapterArchiveDao.getChaptersByVolumePaged(
                        comicId = comicId,
                        volumeId = volumeId,
                        pageSize = pageSize,
                        offset = offset,
                    )
                } else {
                    chapterArchiveDao.getChaptersByVolumePagedDesc(
                        comicId = comicId,
                        volumeId = volumeId,
                        pageSize = pageSize,
                        offset = offset,
                    )
                }
            return joins.map { it.toViewDto() }
        }

        override fun observeHasRootChapters(comicId: Long): Flow<Boolean> =
            chapterArchiveDao.observeRootChaptersCountByDirectoryId(comicId).map { it > 0 }

        companion object {
            private const val TAG = "VolumeArchiveEngine"
        }
    }
