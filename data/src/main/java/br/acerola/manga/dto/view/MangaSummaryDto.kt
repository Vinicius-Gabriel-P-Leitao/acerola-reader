package br.acerola.manga.dto.view

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.manga.pattern.MetadataSourcePattern
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangaSummaryDto(
    val directoryId: Long,
    val folderName: String,
    val folderCover: String?,
    val folderBanner: String?,
    val externalSync: Boolean,
    val metadataTitle: String?,
    val activeSource: MetadataSourcePattern?,
    val metadataId: Long?
) : Parcelable
