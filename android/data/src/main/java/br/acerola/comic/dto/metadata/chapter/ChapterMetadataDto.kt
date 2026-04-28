package br.acerola.comic.dto.metadata.chapter

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ChapterMetadataDto(
    val id: String,
    val volume: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val scanlator: String? = null,
    val pages: Int = 0,
    val mangadexVersion: Int,
    val pageUrls: List<String> = emptyList(),
) : Parcelable
