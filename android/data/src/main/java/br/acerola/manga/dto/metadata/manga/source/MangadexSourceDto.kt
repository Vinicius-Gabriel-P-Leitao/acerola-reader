package br.acerola.manga.dto.metadata.manga.source

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangadexSourceDto(
    val mangadexId: String,
    val anilistId: String? = null,
    val amazonUrl: String? = null,
    val ebookjapanUrl: String? = null,
    val rawUrl: String? = null,
    val engtlUrl: String? = null,
) : Parcelable
