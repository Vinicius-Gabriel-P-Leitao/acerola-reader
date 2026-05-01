package br.acerola.comic.dto.archive

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ComicDirectoryDto(
    val id: Long,
    val name: String,
    val path: String,
    val coverUri: Uri?,
    val bannerUri: Uri?,
    val lastModified: Long,
    val archiveTemplateFk: Long?,
    val externalSyncEnabled: Boolean = true,
    val hidden: Boolean = false,
) : Parcelable
