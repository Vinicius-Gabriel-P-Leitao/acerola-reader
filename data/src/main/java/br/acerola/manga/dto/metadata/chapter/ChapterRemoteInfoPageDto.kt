package br.acerola.manga.dto.metadata.chapter

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ChapterRemoteInfoPageDto(
    val items: List<ChapterFeedDto>,
    val pageSize: Int,
    val page: Int,
    val total: Int
) : Parcelable

@Parcelize
@Immutable
data class ChapterFeedDto(
    val id: Long,
    val title: String,
    val chapter: String,
    val pageCount: Int?,
    val scanlation: String,
    val source: List<ChapterSourceDto>
) : Parcelable

@Parcelize
@Immutable
data class ChapterSourceDto(
    val pageNumber: Int,
    val imageUrl: String,
    val downloaded: Boolean,
) : Parcelable