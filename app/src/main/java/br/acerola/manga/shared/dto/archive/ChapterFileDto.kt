package br.acerola.manga.shared.dto.archive

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ChapterPageDto(
    val items: List<ChapterFileDto>,
    val pageSize: Int,
    val page: Int,
    val total: Int
) : Parcelable

@Parcelize
@Immutable
data class ChapterFileDto(
    val id: Long,
    val name: String,
    val path: String,
    val chapterSort: String
) : Parcelable