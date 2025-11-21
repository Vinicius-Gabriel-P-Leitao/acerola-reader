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
    val gender: List<String> = emptyList(),
    val year: Int? = null,
    val status: String,
    val author: String? = null,
) : Parcelable
