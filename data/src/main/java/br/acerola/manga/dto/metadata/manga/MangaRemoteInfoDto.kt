package br.acerola.manga.dto.metadata.manga

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

// FIXME: Tá sabendo de origem dos dados não pode saber

@Parcelize
@Immutable
data class MangaRemoteInfoDto(
    val id: Long? = null,
    val mangadexId: String? = null,
    val anilistId: String? = null,
    val localHash: String? = null,
    val title: String,
    val description: String,
    val romanji: String? = null,
    val year: Int? = null,
    val status: String,
    val cover: CoverDto? = null,
    val banner: BannerDto? = null,
    val authors: AuthorDto? = null,
    val genre: List<GenreDto> = emptyList(),
    val links: LinksDto? = null,
    val anilistScore: Int? = null,
    val anilistPopularity: Int? = null,
    val anilistTrending: Int? = null,
    val anilistCoverImage: String? = null,
    val anilistBannerImage: String? = null,
    val mangaDirectoryFk: Long? = null
) : Parcelable

@Parcelize
@Immutable
data class BannerDto(
    val id: String,
    val url: String,
    val fileName: String,
) : Parcelable

@Parcelize
@Immutable
data class CoverDto(
    val id: String,
    val url: String,
    val fileName: String,
) : Parcelable

@Parcelize
@Immutable
data class GenreDto(
    val id: String,
    val name: String
) : Parcelable

@Parcelize
@Immutable
data class AuthorDto(
    val id: String,
    val name: String,
    val type: String
) : Parcelable

@Parcelize
@Immutable
data class LinksDto(
    val anilistId: String? = null,
    val amazonUrl: String? = null,
    val ebookjapanUrl: String? = null,
    val rawUrl: String? = null,
    val engtlUrl: String? = null,
) : Parcelable
