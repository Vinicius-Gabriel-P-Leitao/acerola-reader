package br.acerola.comic.dto.history

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ReadingHistoryDto(
    val comicDirectoryId: Long,
    val chapterArchiveId: Long? = null,
    val chapterSort: String,
    val lastPage: Int,
    val isCompleted: Boolean,
    val updatedAt: Long,
) : Parcelable

@Parcelize
@Immutable
data class ReadingHistoryWithChapterDto(
    val comicDirectoryId: Long,
    val chapterArchiveId: Long? = null,
    val chapterSort: String,
    val lastPage: Int,
    val updatedAt: Long,
    val chapterName: String?,
    val isCompleted: Boolean,
) : Parcelable
