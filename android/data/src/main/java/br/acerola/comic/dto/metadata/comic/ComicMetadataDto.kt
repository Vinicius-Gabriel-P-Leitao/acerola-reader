package br.acerola.comic.dto.metadata.comic

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.comic.dto.metadata.comic.source.ComicSourcesDto
import br.acerola.comic.pattern.MetadataSourcePattern
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ComicMetadataDto(
    val id: Long? = null,
    val title: String,
    val description: String,
    val romanji: String? = null,
    val year: Int? = null,
    val status: String,
    val cover: CoverDto? = null,
    val banner: BannerDto? = null,
    val authors: AuthorDto? = null,
    val genre: List<GenreDto> = emptyList(),
    val mangaDirectoryFk: Long? = null,
    val syncSource: MetadataSourcePattern? = null,
    val sources: ComicSourcesDto? = null,
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
    val name: String,
) : Parcelable

@Parcelize
@Immutable
data class AuthorDto(
    val id: String,
    val name: String,
    val type: String,
) : Parcelable
