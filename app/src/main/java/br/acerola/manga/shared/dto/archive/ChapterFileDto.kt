package br.acerola.manga.shared.dto.archive

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChapterFileDto(
    val id: Long,
    val name: String,
    val path: String,
    val chapterSort: String
) : Parcelable