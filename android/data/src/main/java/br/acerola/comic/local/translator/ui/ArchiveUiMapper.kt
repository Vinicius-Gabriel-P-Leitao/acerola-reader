package br.acerola.comic.local.translator.ui

import androidx.core.net.toUri
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.archive.VolumeDto
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ComicDirectory
import br.acerola.comic.local.entity.archive.VolumeArchive
import br.acerola.comic.local.entity.relation.ChapterVolumeJoin

fun ComicDirectory.toViewDto(): ComicDirectoryDto =
    ComicDirectoryDto(
        id = id,
        name = name,
        path = path,
        coverUri = cover?.toUri(),
        bannerUri = banner?.toUri(),
        lastModified = lastModified,
        chapterTemplateFk = chapterTemplateFk,
        externalSyncEnabled = externalSyncEnabled,
        hidden = hidden,
    )

fun VolumeArchive.toViewDto(): VolumeDto =
    VolumeDto(
        id = id,
        name = name,
        volumeSort = volumeSort,
        isSpecial = isSpecial,
        coverUri = cover,
        bannerUri = banner,
    )

fun ChapterArchive.toViewDto(volumeName: String? = null): ChapterFileDto =
    ChapterFileDto(
        id = id,
        name = chapter,
        path = path,
        chapterSort = chapterSort,
        volumeId = volumeIdFk,
        volumeName = volumeName,
        isSpecial = isSpecial,
        lastModified = lastModified,
    )

fun ChapterVolumeJoin.toViewDto(): ChapterFileDto = chapter.toViewDto(volumeName = volume?.name)

fun List<ChapterVolumeJoin>.toViewPageDto(
    pageSize: Int = this.size,
    total: Int = this.size,
    page: Int = 0,
): ChapterArchivePageDto {
    val volumes = this.mapNotNull { it.volume }.distinctBy { it.id }.map { it.toViewDto() }
    return ChapterArchivePageDto(
        items = this.map { it.toViewDto() },
        volumes = volumes,
        pageSize = pageSize,
        total = total,
        page = page,
    )
}

// Fallback for cases where we only have chapters
fun List<ChapterArchive>.toViewPageDtoLegacy(
    pageSize: Int = this.size,
    total: Int = this.size,
    page: Int = 0,
): ChapterArchivePageDto =
    ChapterArchivePageDto(
        items = this.map { it.toViewDto() },
        pageSize = pageSize,
        total = total,
        page = page,
    )
