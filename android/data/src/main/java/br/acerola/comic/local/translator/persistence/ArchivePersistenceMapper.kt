package br.acerola.comic.local.translator.persistence

import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ComicDirectory
import br.acerola.comic.local.entity.archive.VolumeArchive
import br.acerola.comic.util.file.FastFileMetadata

fun ComicDirectoryDto.toEntity(): ComicDirectory =
    ComicDirectory(
        id = id,
        name = name,
        path = path,
        cover = coverUri?.toString(),
        banner = bannerUri?.toString(),
        lastModified = System.currentTimeMillis(),
        archiveTemplateFk = archiveTemplateFk,
        externalSyncEnabled = externalSyncEnabled,
    )

fun ChapterFileDto.toEntity(folderId: Long): ChapterArchive =
    ChapterArchive(
        chapter = name,
        path = path,
        chapterSort = chapterSort,
        folderPathFk = folderId,
        volumeIdFk = volumeId,
        isSpecial = isSpecial,
    )

fun DocumentFile.toMangaDirectoryEntity(
    cover: DocumentFile?,
    banner: DocumentFile?,
    archiveTemplateFk: Long?,
    externalSyncEnabled: Boolean = true,
): ComicDirectory =
    ComicDirectory(
        name = name ?: "Unknown",
        path = uri.toString(),
        cover = cover?.uri?.toString(),
        banner = banner?.uri?.toString(),
        archiveTemplateFk = archiveTemplateFk,
        lastModified = lastModified(),
        externalSyncEnabled = externalSyncEnabled,
    )

fun FastFileMetadata.toChapterArchiveEntity(
    comicId: Long,
    chapterSort: String,
    fileUri: String,
    fastHash: String,
    volumeIdFk: Long? = null,
    isSpecial: Boolean = false,
): ChapterArchive =
    ChapterArchive(
        chapter = name,
        path = fileUri,
        checksum = null,
        fastHash = fastHash,
        chapterSort = chapterSort,
        folderPathFk = comicId,
        volumeIdFk = volumeIdFk,
        isSpecial = isSpecial,
        lastModified = lastModified,
    )

fun FastFileMetadata.toVolumeArchiveEntity(
    comicId: Long,
    volumeSort: String,
    folderUri: String,
    isSpecial: Boolean,
    coverPath: String? = null,
    bannerPath: String? = null,
): VolumeArchive =
    VolumeArchive(
        name = name,
        path = folderUri,
        volumeSort = volumeSort,
        isSpecial = isSpecial,
        cover = coverPath,
        banner = bannerPath,
        comicDirectoryFk = comicId,
        lastModified = lastModified,
    )

fun FastFileMetadata.toMangaDirectoryEntity(
    folderUri: String,
    coverPath: String?,
    bannerPath: String?,
    archiveTemplateFk: Long?,
): ComicDirectory =
    ComicDirectory(
        name = name,
        path = folderUri,
        cover = coverPath,
        banner = bannerPath,
        archiveTemplateFk = archiveTemplateFk,
        lastModified = lastModified,
    )

fun DocumentFile.toChapterArchiveEntity(
    comicId: Long,
    chapterSort: String,
    checksum: String?,
    fastHash: String?,
    volumeIdFk: Long? = null,
    isSpecial: Boolean = false,
): ChapterArchive =
    ChapterArchive(
        chapter = name ?: "",
        path = uri.toString(),
        checksum = checksum,
        fastHash = fastHash,
        chapterSort = chapterSort,
        folderPathFk = comicId,
        volumeIdFk = volumeIdFk,
        isSpecial = isSpecial,
        lastModified = lastModified(),
    )

fun DocumentFile.toDto(chapterSort: String = "0"): ChapterFileDto =
    ChapterFileDto(
        id = 0,
        name = name ?: "Unknown",
        path = uri.toString(),
        chapterSort = chapterSort,
        lastModified = lastModified(),
    )
