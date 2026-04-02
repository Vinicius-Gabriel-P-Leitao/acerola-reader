package br.acerola.manga.local.translator.ui

import androidx.core.net.toUri
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.local.entity.archive.ChapterArchive
import br.acerola.manga.local.entity.archive.MangaDirectory

fun MangaDirectory.toViewDto(): MangaDirectoryDto {
    return MangaDirectoryDto(
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
