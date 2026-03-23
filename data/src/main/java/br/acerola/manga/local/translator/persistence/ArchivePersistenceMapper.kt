package br.acerola.manga.local.translator.persistence

import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.local.entity.archive.ChapterArchive
import br.acerola.manga.local.entity.archive.MangaDirectory
import br.acerola.manga.util.FastFileMetadata

fun MangaDirectoryDto.toEntity(): MangaDirectory {
    return MangaDirectory(
        id = id,
        name = name,
        path = path,
        cover = coverUri?.toString(),
        banner = bannerUri?.toString(),
        lastModified = System.currentTimeMillis(),
        chapterTemplateFk = chapterTemplateFk,
        externalSyncEnabled = externalSyncEnabled
    )
}

fun ChapterFileDto.toEntity(folderId: Long): ChapterArchive {
    return ChapterArchive(
        chapter = name, path = path, chapterSort = chapterSort, folderPathFk = folderId
    )
}

fun DocumentFile.toMangaDirectoryEntity(
    cover: DocumentFile?, banner: DocumentFile?, chapterTemplateFk: Long?, externalSyncEnabled: Boolean = true
): MangaDirectory {
    return MangaDirectory(
        name = name ?: "Unknown",
        path = uri.toString(),
        cover = cover?.uri?.toString(),
        banner = banner?.uri?.toString(),
        chapterTemplateFk = chapterTemplateFk,
        lastModified = lastModified(),
        externalSyncEnabled = externalSyncEnabled,
    )
}

fun FastFileMetadata.toChapterArchiveEntity(
    mangaId: Long, chapterSort: String, fileUri: String, fastHash: String
): ChapterArchive {
    return ChapterArchive(
        chapter = name,
        path = fileUri,
        checksum = null,
        fastHash = fastHash,
        chapterSort = chapterSort,
        folderPathFk = mangaId
    )
}

fun FastFileMetadata.toMangaDirectoryEntity(
    folderUri: String,
    coverPath: String?,
    bannerPath: String?,
    chapterTemplateFk: Long?
): MangaDirectory {
    return MangaDirectory(
        name = name,
        path = folderUri,
        cover = coverPath,
        banner = bannerPath,
        chapterTemplateFk = chapterTemplateFk,
        lastModified = lastModified,
    )
}

fun DocumentFile.toChapterArchiveEntity(
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
