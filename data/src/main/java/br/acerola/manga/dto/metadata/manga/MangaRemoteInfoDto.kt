package br.acerola.manga.dto.metadata.manga

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.local.database.entity.metadata.MetadataSource
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangaRemoteInfoDto(
    val id: Long? = null,
    val mirrorId: String,
    val title: String,
    val description: String,
    val romanji: String? = null,
    val year: Int? = null,
    val status: String,
    val cover: CoverDto? = null,
    val authors: AuthorDto? = null,
    val genre: List<GenreDto> = emptyList(),
    val metadataSource: MetadataSource = MetadataSource.MANGADEX
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
