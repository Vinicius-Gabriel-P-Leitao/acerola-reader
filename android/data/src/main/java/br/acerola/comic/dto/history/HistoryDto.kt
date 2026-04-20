package br.acerola.comic.dto.history

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ReadingHistoryDto(
    val mangaDirectoryId: Long,
    val chapterArchiveId: Long,
    val lastPage: Int,
    val isCompleted: Boolean,
    val updatedAt: Long,
) : Parcelable

@Parcelize
@Immutable
data class ReadingHistoryWithChapterDto(
    val mangaDirectoryId: Long,
    val chapterArchiveId: Long,
    val lastPage: Int,
    val updatedAt: Long,
    val chapterName: String?,
    val isCompleted: Boolean,
) : Parcelable
