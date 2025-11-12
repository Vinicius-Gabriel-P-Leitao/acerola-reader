package br.acerola.manga.shared.dto.archive

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MangaFolderDto(
    val id: Long,
    val name: String,
    val path: String,
    val coverUri: Uri?,
    val bannerUri: Uri?,
    val lastModified: Long,
    val chapterTemplate: String?,
    val chapters: List<ChapterFileDto>
) : Parcelable