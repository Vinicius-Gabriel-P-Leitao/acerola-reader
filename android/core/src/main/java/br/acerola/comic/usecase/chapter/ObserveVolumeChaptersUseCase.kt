package br.acerola.comic.usecase.chapter

import br.acerola.comic.adapter.contract.gateway.VolumeGateway
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import kotlinx.coroutines.flow.Flow

class ObserveVolumeChaptersUseCase(
    private val volumeGateway: VolumeGateway,
) {

    fun observeByComic(
        comicId: Long,
        previewSize: Int = 5,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): Flow<List<VolumeChapterGroupDto>> =
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

    fun observeHasRootChapters(comicId: Long): Flow<Boolean> = volumeGateway.observeHasRootChapters(comicId)
}
