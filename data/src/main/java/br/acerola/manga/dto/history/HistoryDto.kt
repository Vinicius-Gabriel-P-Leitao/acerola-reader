package br.acerola.manga.dto.history

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ReadingHistoryDto(
    val mangaId: Long,
    val chapterId: Long,
    val lastPage: Int,
    val updatedAt: Long
) : Parcelable

@Parcelize
@Immutable
data class ReadingHistoryWithChapterDto(
    val mangaId: Long,
    val chapterId: Long,
    val lastPage: Int,
    val updatedAt: Long,
    val chapterName: String?
) : Parcelable
