package br.acerola.comic.local.translator.ui

import androidx.core.net.toUri
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ComicDirectory

fun ComicDirectory.toViewDto(): ComicDirectoryDto {
    return ComicDirectoryDto(
        id = id,
        name = name,
        path = path,
        coverUri = cover?.toUri(),
        bannerUri = banner?.toUri(),
        lastModified = lastModified,
        chapterTemplateFk = chapterTemplateFk,
        externalSyncEnabled = externalSyncEnabled,
        hidden = hidden
    )
}

fun ChapterArchive.toViewDto(): ChapterFileDto {
    return ChapterFileDto(
        id = id,
        name = chapter,
        path = path,
        chapterSort = chapterSort,
        lastModified = lastModified
    )
}

fun List<ChapterArchive>.toViewPageDto(
    pageSize: Int = this.size, total: Int = this.size, page: Int = 0
): ChapterArchivePageDto {
    return ChapterArchivePageDto(
        items = this.map { it.toViewDto() }, pageSize = pageSize, total = total, page = page
    )
}
