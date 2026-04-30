package br.acerola.comic.dto.archive

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ChapterArchivePageDto(
    val items: List<ChapterFileDto>,
    val volumes: List<VolumeDto> = emptyList(),
    val pageSize: Int,
    val page: Int,
    val total: Int,
    val volumeSections: List<VolumeChapterGroupDto> = emptyList(),
) : Parcelable

@Parcelize
@Immutable
data class VolumeChapterGroupDto(
    val volume: VolumeDto,
    val items: List<ChapterFileDto>,
    val totalChapters: Int,
    val loadedCount: Int,
    val hasMore: Boolean,
) : Parcelable

@Parcelize
@Immutable
data class ChapterFileDto(
    val id: Long,
    val name: String,
    val path: String,
    val chapterSort: String,
    val volumeId: Long? = null,
    val volumeName: String? = null,
    val isSpecial: Boolean = false,
    val lastModified: Long = 0,
) : Parcelable
