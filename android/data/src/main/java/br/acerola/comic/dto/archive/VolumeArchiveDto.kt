package br.acerola.comic.dto.archive

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

// FIXME: Nome errado, correto deve ser VolumeArchiveDto
@Parcelize
@Immutable
data class VolumeArchiveDto(
    val id: Long,
    val name: String,
    val volumeSort: String,
    val isSpecial: Boolean,
    val coverUri: String? = null,
    val bannerUri: String? = null,
) : Parcelable
