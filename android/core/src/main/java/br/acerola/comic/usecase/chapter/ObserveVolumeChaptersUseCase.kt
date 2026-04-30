package br.acerola.comic.usecase.chapter

import br.acerola.comic.adapter.contract.gateway.VolumeChapterGateway
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import kotlinx.coroutines.flow.StateFlow

class ObserveVolumeChaptersUseCase(
    private val volumeGateway: VolumeChapterGateway,
) {
    fun observeByComic(
        comicId: Long,
        previewSize: Int = 5,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): StateFlow<List<VolumeChapterGroupDto>> =
        volumeGateway.observeVolumeGroups(
            comicId = comicId,
            previewSize = previewSize,
            sortType = sortType,
            isAscending = isAscending,
        )

    suspend fun loadVolumePage(
        comicId: Long,
        volumeId: Long,
        offset: Int,
        pageSize: Int = 20,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): List<ChapterFileDto> =
        volumeGateway.getVolumeChapterPage(
            comicId = comicId,
            volumeId = volumeId,
            offset = offset,
            pageSize = pageSize,
            sortType = sortType,
            isAscending = isAscending,
        )

    suspend fun hasRootChapters(comicId: Long): Boolean = volumeGateway.hasRootChapters(comicId)
}
