package br.acerola.manga.shared.dto.archive

import android.net.Uri

data class MangaFolderDto(
    val name: String,
    val path: String,
    val coverUri: Uri?,
    val bannerUri: Uri?,
    val chapters: List<ChapterFileDto>
)