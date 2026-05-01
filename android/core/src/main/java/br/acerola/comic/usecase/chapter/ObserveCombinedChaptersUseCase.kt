package br.acerola.comic.usecase.chapter

import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.VolumeGateway
import br.acerola.comic.adapter.library.DirectoryEngine
import br.acerola.comic.adapter.metadata.mangadex.MangadexEngine
import br.acerola.comic.config.preference.types.ChapterSortPreferenceData
import br.acerola.comic.config.preference.types.SortDirection
import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.local.translator.ui.toCombinedRegularDto
import br.acerola.comic.local.translator.ui.toCombinedVolumeDto
import br.acerola.comic.service.cache.ChapterCacheHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * UseCase especializado para a ComicViewModel.
 * Orquestra dados locais, remotos e volumes com Cache LRU.
 */
class ObserveCombinedChaptersUseCase
    @Inject
    constructor(
        @param:DirectoryEngine private val volumeGateway: VolumeGateway,
        @param:DirectoryEngine private val localRepository: ChapterGateway<ChapterPageDto>,
        @param:MangadexEngine private val remoteRepository: ChapterGateway<ChapterRemoteInfoPageDto>,
        private val cacheHandler: ChapterCacheHandler,
    ) {
        val progress: StateFlow<Int> get() = localRepository.progress
        val isIndexing: StateFlow<Boolean> get() = localRepository.isIndexing

        fun observeCombined(
            comicId: Long,
            remoteId: Long?,
            sort: ChapterSortPreferenceData,
            page: Int,
            pageSize: Int,
            viewMode: VolumeViewType,
            volumeOverrides: Map<Long, VolumeChapterGroupDto>,
        ): Flow<ChapterDto?> {
            val cacheKey =
                cacheHandler.generateKey(
                    comicId,
                    sort.type.name,
                    sort.direction == SortDirection.ASCENDING,
                    pageSize,
                    viewMode.name,
                    page,
                    volumeOverrides.hashCode().toString(),
                )

            val localFlow =
                localRepository
                    .observeChapters(comicId, sort.type.name, sort.direction == SortDirection.ASCENDING)
                    .filter { it.pageSize != -1 }

            val volumeFlow =
                volumeGateway
                    .observeVolumeGroups(comicId, pageSize, sort.type.name, sort.direction == SortDirection.ASCENDING)
                    .filter { it.firstOrNull()?.totalChapters != -1 }

            val remoteFlow =
                if (remoteId != null) {
                    remoteRepository
                        .observeChapters(remoteId, sort.type.name, sort.direction == SortDirection.ASCENDING)
                        .filter { it.pageSize != -1 }
                } else {
                    flowOf(ChapterRemoteInfoPageDto(emptyList(), 0, 0, 0))
                }

            return combine(
                localFlow,
                volumeFlow,
                remoteFlow,
                volumeGateway.observeHasRootChapters(comicId),
            ) { localAll, volumeSections, remoteAll, hasRootChapters ->
                val isInitialState = localAll.pageSize == 0 && localAll.items.isEmpty()

                if (!isInitialState) {
                    val cached = cacheHandler.get(cacheKey)

                    if (cached != null) {
                        if (cached.archive.items.isNotEmpty() || localAll.items.isEmpty()) {
                            return@combine cached
                        }
                    }
                }

                val hasSubfolders = volumeSections.isNotEmpty()

                val effectiveViewMode =
                    if (hasSubfolders) {
                        if (viewMode == VolumeViewType.COVER_VOLUME) VolumeViewType.COVER_VOLUME else VolumeViewType.VOLUME
                    } else {
                        VolumeViewType.CHAPTER
                    }

                val result =
                    if (effectiveViewMode != VolumeViewType.CHAPTER) {
                        volumeSections.toCombinedVolumeDto(
                            remoteAll = remoteAll,
                            volumeOverrides = volumeOverrides,
                            pageSize = pageSize,
                            effectiveViewMode = effectiveViewMode,
                        )
                    } else {
                        localAll.toCombinedRegularDto(
                            remoteAll = remoteAll,
                            page = page,
                            pageSize = pageSize,
                            hasVolumeStructure = hasSubfolders,
                            effectiveViewMode = effectiveViewMode,
                        )
                    }

                // Only cache valid results
                if (!isInitialState) {
                    cacheHandler.put(cacheKey, result)
                }
                result
            }
        }
    }