package br.acerola.manga.shared.dto.archive

import android.net.Uri

data class MangaFolderDto(
    val id: Long,
    val name: String,
    val path: String,
    val coverUri: Uri?,
    val bannerUri: Uri?,
    val lastModified: Long,
    val chapters: List<ChapterFileDto>
)