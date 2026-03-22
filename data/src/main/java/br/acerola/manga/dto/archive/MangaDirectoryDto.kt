package br.acerola.manga.dto.archive

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangaDirectoryDto(
    val id: Long,
    val name: String,
    val path: String,
    val coverUri: Uri?,
    val bannerUri: Uri?,
    val lastModified: Long,
    val chapterTemplate: String?,
    val hasComicInfo: Boolean = false,
    val externalSyncEnabled: Boolean = true,
    ) : Parcelable