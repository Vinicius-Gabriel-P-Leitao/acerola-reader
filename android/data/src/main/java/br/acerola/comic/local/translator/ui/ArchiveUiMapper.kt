package br.acerola.comic.local.translator.ui

import androidx.core.net.toUri
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.archive.VolumeArchiveDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ComicDirectory
import br.acerola.comic.local.entity.archive.VolumeArchive
import br.acerola.comic.local.entity.relation.ChapterVolumeJoin
import br.acerola.comic.local.entity.relation.VolumeChapterCount
import br.acerola.comic.util.sort.normalizeSort
import kotlin.math.ceil
import kotlin.math.max

fun ComicDirectory.toViewDto(): ComicDirectoryDto =
    ComicDirectoryDto(
        id = id,
        name = name,
        path = path,
        coverUri = cover?.toUri(),
        bannerUri = banner?.toUri(),
        lastModified = lastModified,
        archiveTemplateFk = archiveTemplateFk,
        externalSyncEnabled = externalSyncEnabled,
        hidden = hidden,
    )

fun VolumeArchive.toViewDto(): VolumeArchiveDto =
    VolumeArchiveDto(
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

fun VolumeChapterCount.toViewDto(): VolumeArchiveDto =
    VolumeArchiveDto(
        id = id,
        name = name,
        volumeSort = volumeSort,
        isSpecial = isSpecial,
        coverUri = cover,
        bannerUri = banner,
    )

fun VolumeChapterCount.toGroupDto(items: List<ChapterFileDto>): VolumeChapterGroupDto =
    VolumeChapterGroupDto(
        volume = toViewDto(),
        items = items,
        totalChapters = chapterCount,
        loadedCount = items.size,
        hasMore = items.size < chapterCount,
    )

fun List<ChapterVolumeJoin>.toViewPageDto(
    pageSize: Int = this.size,
    total: Int = this.size,
    page: Int = 0,
): ChapterPageDto {
    val volumes = this.mapNotNull { it.volume }.distinctBy { it.id }.map { it.toViewDto() }
    return ChapterPageDto(
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
): ChapterPageDto =
    ChapterPageDto(
        items = this.map { it.toViewDto() },
        pageSize = pageSize,
        total = total,
        page = page,
    )

fun List<VolumeChapterGroupDto>.toCombinedVolumeDto(
    remoteAll: ChapterRemoteInfoPageDto,
    volumeOverrides: Map<Long, VolumeChapterGroupDto>,
    pageSize: Int,
): ChapterDto {
    val mergedSections = this.map { section ->
        val sectionTotalPages = if (section.totalChapters == 0) 1
        else ceil(section.totalChapters.toDouble() / pageSize).toInt()

        volumeOverrides[section.volume.id]?.let { override ->
            section.copy(
                items = override.items,
                loadedCount = override.items.size,
                hasMore = override.items.size < section.totalChapters,
                currentPage = override.currentPage,
                totalPages = sectionTotalPages,
            )
        } ?: section.copy(totalPages = sectionTotalPages)
    }

    val visibleItems = mergedSections.flatMap { it.items }
    val remoteMap = remoteAll.items.associateBy { it.chapter.normalizeSort() }

    val filteredRemoteItems = visibleItems.mapNotNull { local ->
        remoteMap[local.chapterSort.normalizeSort()]
    }

    return ChapterDto(
        archive = ChapterPageDto(
            items = visibleItems,
            volumes = mergedSections.map { it.volume },
            volumeSections = mergedSections,
            pageSize = pageSize,
            total = mergedSections.sumOf { it.totalChapters },
            page = 0,
        ),
        remoteInfo = ChapterRemoteInfoPageDto(filteredRemoteItems, pageSize, 0, visibleItems.size),
        showVolumeHeaders = mergedSections.size > 1,
        hasVolumeStructure = true,
    )
}

fun ChapterPageDto.toCombinedRegularDto(
    remoteAll: ChapterRemoteInfoPageDto,
    page: Int,
    pageSize: Int,
    hasVolumeStructure: Boolean,
): ChapterDto {
    val items = this.items
    if (items.isEmpty()) {
        return ChapterDto(
            archive = this,
            remoteInfo = remoteAll,
            hasVolumeStructure = hasVolumeStructure,
        )
    }

    val total = items.size
    val totalPages = ceil(total.toDouble() / pageSize).toInt()
    val safePage = page.coerceIn(0, max(0, totalPages - 1))

    // Accumulative end index for infinite scroll
    val end = ((safePage + 1) * pageSize).coerceIn(0, total)

    val pagedLocalItems = items.subList(0, end)
    val remoteMap = remoteAll.items.associateBy { it.chapter.normalizeSort() }

    val filteredRemoteItems = pagedLocalItems.mapNotNull { local ->
        remoteMap[local.chapterSort.normalizeSort()]
    }

    return ChapterDto(
        archive = ChapterPageDto(
            items = pagedLocalItems,
            volumes = this.volumes,
            pageSize = pageSize,
            total = total,
            page = safePage,
        ),
        remoteInfo = ChapterRemoteInfoPageDto(filteredRemoteItems, pageSize, safePage, total),
        showVolumeHeaders = false,
        hasVolumeStructure = hasVolumeStructure,
    )
}
