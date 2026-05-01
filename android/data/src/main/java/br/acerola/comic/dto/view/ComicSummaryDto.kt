package br.acerola.comic.dto.view

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.comic.pattern.metadata.MetadataSource
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ComicSummaryDto(
    val directoryId: Long,
    val folderName: String,
    val folderCover: String?,
    val folderBanner: String?,
    val externalSync: Boolean,
    val metadataTitle: String?,
    val activeSource: MetadataSource?,
    val metadataId: Long?,
) : Parcelable
