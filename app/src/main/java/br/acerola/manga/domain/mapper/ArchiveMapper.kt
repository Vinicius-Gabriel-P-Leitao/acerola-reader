package br.acerola.manga.domain.mapper

import androidx.core.net.toUri
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.MangaFolderDto

fun MangaFolder.toDto(chapters: List<ChapterFile>): MangaFolderDto {
    return MangaFolderDto(
        id = id,
        name = name,
        path = path,
        lastModified = lastModified,
        coverUri = cover?.toUri(),
        bannerUri = banner?.toUri(),
        chapters = chapters.map { it.toDto() })
}

fun MangaFolderDto.toModel(): MangaFolder {
    return MangaFolder(
        name = name,
        path = path,
        cover = coverUri?.toString(),
        banner = bannerUri?.toString(),
        lastModified = System.currentTimeMillis()
    )
}

fun ChapterFile.toDto(): ChapterFileDto {
    return ChapterFileDto(
        id = id,
        name = chapter,
        path = path
    )
}

fun ChapterFileDto.toModel(folderId: Long): ChapterFile {
    return ChapterFile(
        chapter = name,
        path = path,
        folderPathFk = folderId
    )
}