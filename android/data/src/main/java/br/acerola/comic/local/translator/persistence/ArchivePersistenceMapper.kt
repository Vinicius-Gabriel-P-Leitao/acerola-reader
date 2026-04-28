package br.acerola.comic.local.translator.persistence

import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ComicDirectory
import br.acerola.comic.util.FastFileMetadata

fun ComicDirectoryDto.toEntity(): ComicDirectory =
    ComicDirectory(
        id = id,
        name = name,
        path = path,
        cover = coverUri?.toString(),
        banner = bannerUri?.toString(),
        lastModified = System.currentTimeMillis(),
        chapterTemplateFk = chapterTemplateFk,
        externalSyncEnabled = externalSyncEnabled,
    )

fun ChapterFileDto.toEntity(folderId: Long): ChapterArchive =
    ChapterArchive(
        chapter = name,
        path = path,
        chapterSort = chapterSort,
        folderPathFk = folderId,
    )

fun DocumentFile.toMangaDirectoryEntity(
    cover: DocumentFile?,
    banner: DocumentFile?,
    chapterTemplateFk: Long?,
    externalSyncEnabled: Boolean = true,
): ComicDirectory =
    ComicDirectory(
        name = name ?: "Unknown",
        path = uri.toString(),
        cover = cover?.uri?.toString(),
        banner = banner?.uri?.toString(),
        chapterTemplateFk = chapterTemplateFk,
        lastModified = lastModified(),
        externalSyncEnabled = externalSyncEnabled,
    )

fun FastFileMetadata.toChapterArchiveEntity(
    mangaId: Long,
    chapterSort: String,
    fileUri: String,
    fastHash: String,
): ChapterArchive =
    ChapterArchive(
        chapter = name,
        path = fileUri,
        checksum = null,
        fastHash = fastHash,
        chapterSort = chapterSort,
        folderPathFk = mangaId,
        lastModified = lastModified,
    )

fun FastFileMetadata.toMangaDirectoryEntity(
    folderUri: String,
    coverPath: String?,
    bannerPath: String?,
    chapterTemplateFk: Long?,
): ComicDirectory =
    ComicDirectory(
        name = name,
        path = folderUri,
        cover = coverPath,
        banner = bannerPath,
        chapterTemplateFk = chapterTemplateFk,
        lastModified = lastModified,
    )

fun DocumentFile.toChapterArchiveEntity(
    mangaId: Long,
    chapterSort: String,
    checksum: String?,
    fastHash: String?,
): ChapterArchive =
    ChapterArchive(
        chapter = name ?: "",
        path = uri.toString(),
        checksum = checksum,
        fastHash = fastHash,
        chapterSort = chapterSort,
        folderPathFk = mangaId,
        lastModified = lastModified(),
    )
