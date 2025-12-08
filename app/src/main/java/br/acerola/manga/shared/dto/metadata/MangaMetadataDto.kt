package br.acerola.manga.shared.dto.metadata

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangaMetadataDto(
    val id: String,
    val title: String,
    val description: String,
    val romanji: String? = null,
    val year: Int? = null,
    val status: String,
    val cover: CoverDto? = null,
    val authors: AuthorDto? = null,
    val gender: List<GenreDto> = emptyList(),
) : Parcelable

@Parcelize
@Immutable
data class CoverDto(
    val id: String,
    val fileName: String,
    val url: String
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