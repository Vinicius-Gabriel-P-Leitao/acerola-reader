package br.acerola.manga.local.mapper

import androidx.core.net.toUri
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.ChapterPageDto
import br.acerola.manga.dto.archive.MangaFolderDto
import br.acerola.manga.local.database.entity.archive.ChapterFile
import br.acerola.manga.local.database.entity.archive.MangaFolder

fun MangaFolder.toDto(firstPage: ChapterPageDto): MangaFolderDto {
    return MangaFolderDto(
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

fun MangaFolderDto.toModel(): MangaFolder {
    return MangaFolder(
        name = name,
        path = path,
        cover = coverUri?.toString(),
        banner = bannerUri?.toString(),
        lastModified = System.currentTimeMillis(),
        chapterTemplate = chapterTemplate
    )
}

fun ChapterFile.toDto(): ChapterFileDto {
    return ChapterFileDto(
        id = id,
        name = chapter,
        path = path,
        chapterSort = chapterSort
    )
}

fun ChapterFileDto.toModel(folderId: Long): ChapterFile {
    return ChapterFile(
        chapter = name,
        path = path,
        chapterSort = chapterSort,
        folderPathFk = folderId
    )
}