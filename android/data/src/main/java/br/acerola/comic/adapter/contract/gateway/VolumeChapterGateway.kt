package br.acerola.comic.adapter.contract.gateway

import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VolumeChapterGateway {
    fun observeVolumeGroups(
        comicId: Long,
        previewSize: Int = 5,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): StateFlow<List<VolumeChapterGroupDto>>

    suspend fun getVolumeChapterPage(
        comicId: Long,
        volumeId: Long,
        offset: Int,
        pageSize: Int = 20,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): List<ChapterFileDto>

    fun observeHasRootChapters(comicId: Long): Flow<Boolean>
}
