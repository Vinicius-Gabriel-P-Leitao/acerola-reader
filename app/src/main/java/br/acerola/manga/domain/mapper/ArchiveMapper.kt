package br.acerola.manga.domain.mapper

import androidx.core.net.toUri
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.ChapterPageDto
import br.acerola.manga.shared.dto.archive.MangaFolderDto

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