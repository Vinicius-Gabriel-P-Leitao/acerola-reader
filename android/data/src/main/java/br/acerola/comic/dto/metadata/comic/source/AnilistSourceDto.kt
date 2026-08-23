package br.acerola.comic.dto.metadata.comic.source

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class AnilistSourceDto(
    val anilistId: Int,
    val averageScore: Int? = null,
    val popularity: Int? = null,
    val trending: Int? = null,
    val coverImage: String? = null,
    val bannerImage: String? = null,
) : Parcelable
