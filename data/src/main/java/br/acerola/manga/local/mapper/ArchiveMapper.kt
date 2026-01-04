package br.acerola.manga.local.mapper

import androidx.core.net.toUri
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.database.entity.archive.MangaDirectory

fun MangaDirectory.toDto(firstPage: ChapterArchivePageDto): MangaDirectoryDto {
    return MangaDirectoryDto(
        id = id,
        name = name,
        path = path,
        coverUri = cover?.toUri(),
        bannerUri = banner?.toUri(),
        lastModified = lastModified,
        chapterTemplate = chapterTemplate,
        chapters = firstPage
    )
}

fun ChapterArchive.toDto(): ChapterFileDto {
    return ChapterFileDto(
        id = id,
        name = chapter,
        path = path,
        chapterSort = chapterSort
    )
}

fun MangaDirectoryDto.toModel(): MangaDirectory {
    return MangaDirectory(
        name = name,
        path = path,
        cover = coverUri?.toString(),
        banner = bannerUri?.toString(),
        lastModified = System.currentTimeMillis(),
        chapterTemplate = chapterTemplate
    )
}

fun ChapterFileDto.toModel(folderId: Long): ChapterArchive {
    return ChapterArchive(
        chapter = name,
        path = path,
        chapterSort = chapterSort,
        folderPathFk = folderId
    )
}