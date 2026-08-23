package br.acerola.comic.dto.metadata.comic.source

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ComicInfoSourceDto(
    val localHash: String? = null,
) : Parcelable
