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
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * UseCase genérico para operações simples de capítulos.
 * Mantido para compatibilidade com Reader e outras ViewModels.
 */
open class ObserveChaptersUseCase<T>(
    private val chapterRepository: ChapterGateway<T>,
) {
    val progress: StateFlow<Int> get() = chapterRepository.progress
    val isIndexing: StateFlow<Boolean> get() = chapterRepository.isIndexing

    fun observeByComic(
        comicId: Long,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): StateFlow<T> = chapterRepository.observeChapters(comicId, sortType, isAscending)

    suspend fun loadPage(
        comicId: Long,
        total: Int,
        page: Int,
        pageSize: Int = 20,
        sortType: String = "NUMBER",
        isAscending: Boolean = true,
    ): T = chapterRepository.getChapterPage(comicId, total, page, pageSize, sortType, isAscending)
}

/**
 * UseCase especializado para a ComicViewModel.
 * Orquestra dados locais, remotos e volumes com Cache LRU.
 */
class ObserveCombinedChaptersUseCase @Inject constructor(
    @param:DirectoryEngine private val volumeGateway: VolumeGateway,
    @param:DirectoryEngine private val localRepository: ChapterGateway<ChapterPageDto>,
    @param:MangadexEngine private val remoteRepository: ChapterGateway<ChapterRemoteInfoPageDto>,
    private val cacheHandler: ChapterCacheHandler
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
        volumeOverrides: Map<Long, VolumeChapterGroupDto>
    ): Flow<ChapterDto?> {
        val cacheKey = cacheHandler.generateKey(
            comicId,
            sort.type.name,
            sort.direction == SortDirection.ASCENDING,
            pageSize,
            viewMode.name,
            page,
            volumeOverrides.hashCode().toString()
        )

        val localFlow = localRepository.observeChapters(comicId, sort.type.name, sort.direction == SortDirection.ASCENDING)
        val remoteFlow = if (remoteId != null) {
            remoteRepository.observeChapters(remoteId, sort.type.name, sort.direction == SortDirection.ASCENDING)
        } else {
            flowOf(ChapterRemoteInfoPageDto(emptyList(), 0, 0, 0))
        }

        return combine(
            localFlow,
            volumeGateway.observeVolumeGroups(comicId, pageSize, sort.type.name, sort.direction == SortDirection.ASCENDING),
            remoteFlow,
            volumeGateway.observeHasRootChapters(comicId)
        ) { localAll, volumeSections, remoteAll, hasRootChapters ->
            val cached = cacheHandler.get(cacheKey)
            if (cached != null) return@combine cached

            val result = if ((viewMode == VolumeViewType.VOLUME || viewMode == VolumeViewType.COVER_VOLUME) &&
                volumeSections.isNotEmpty()
            ) {
                volumeSections.toCombinedVolumeDto(remoteAll, volumeOverrides, pageSize)
            } else {
                localAll.toCombinedRegularDto(remoteAll, page, pageSize, volumeSections.isNotEmpty())
            }

            cacheHandler.put(cacheKey, result)
            result
        }
    }
}
