package br.acerola.manga.dto.metadata.manga.source

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangaSourcesDto(
    val mangadex: MangadexSourceDto? = null,
    val anilist: AnilistSourceDto? = null,
    val comicInfo: ComicInfoSourceDto? = null
) : Parcelable
