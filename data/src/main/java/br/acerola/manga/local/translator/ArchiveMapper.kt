package br.acerola.manga.local.translator

import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.local.entity.archive.ChapterArchive
import br.acerola.manga.local.entity.archive.MangaDirectory

fun MangaDirectory.toDto(): MangaDirectoryDto {
    return MangaDirectoryDto(
        id = id,
        name = name,
        path = path,
        coverUri = cover?.toUri(),
        bannerUri = banner?.toUri(),
        lastModified = lastModified,
        chapterTemplate = chapterTemplate,
        hasComicInfo = hasComicInfo,
        externalSyncEnabled = externalSyncEnabled,
    )
}

fun ChapterArchive.toDto(): ChapterFileDto {
    return ChapterFileDto(
        id = id, name = chapter, path = path, chapterSort = chapterSort
    )
}

fun MangaDirectoryDto.toModel(): MangaDirectory {
    return MangaDirectory(
        name = name,
        path = path,
        cover = coverUri?.toString(),
        banner = bannerUri?.toString(),
        lastModified = System.currentTimeMillis(),
        chapterTemplate = chapterTemplate,
        hasComicInfo = hasComicInfo,
        externalSyncEnabled = externalSyncEnabled
    )
}

fun ChapterFileDto.toModel(folderId: Long): ChapterArchive {
    return ChapterArchive(
        chapter = name, path = path, chapterSort = chapterSort, folderPathFk = folderId
    )
}

fun List<ChapterArchive>.toPageDto(
    pageSize: Int = this.size, total: Int = this.size, page: Int = 0
): ChapterArchivePageDto {
    return ChapterArchivePageDto(
        items = this.map { it.toDto() }, pageSize = pageSize, total = total, page = page
    )
}

fun DocumentFile.toMangaDirectoryModel(
    cover: DocumentFile?, banner: DocumentFile?, chapterTemplate: String?, hasComicInfo: Boolean, externalSyncEnabled: Boolean = true
): MangaDirectory {
    return MangaDirectory(
        name = name ?: "Unknown",
        path = uri.toString(),
        cover = cover?.uri?.toString(),
        banner = banner?.uri?.toString(),
        chapterTemplate = chapterTemplate,
        lastModified = lastModified(),
        hasComicInfo = hasComicInfo,
        externalSyncEnabled = externalSyncEnabled,
    )
}

fun DocumentFile.toChapterArchiveModel(
    mangaId: Long, chapterSort: String, checksum: String?, fastHash: String?
): ChapterArchive {
    return ChapterArchive(
        chapter = name ?: "",
        path = uri.toString(),
        checksum = checksum,
        fastHash = fastHash,
        chapterSort = chapterSort,
        folderPathFk = mangaId
    )
}
