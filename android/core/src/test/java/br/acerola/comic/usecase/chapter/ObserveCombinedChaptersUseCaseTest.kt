package br.acerola.comic.usecase.chapter

import br.acerola.comic.adapter.contract.gateway.ChapterReadGateway
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.adapter.contract.gateway.VolumeGateway
import br.acerola.comic.config.preference.types.ChapterSortPreferenceData
import br.acerola.comic.config.preference.types.ChapterSortType
import br.acerola.comic.config.preference.types.SortDirection
import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.service.cache.ChapterCacheHandler
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ObserveCombinedChaptersUseCaseTest {
    @MockK
    lateinit var volumeGateway: VolumeGateway

    @MockK
    lateinit var localReadGateway: ChapterReadGateway<ChapterPageDto>

    @MockK
    lateinit var localSyncStatusGateway: ChapterSyncStatusGateway

    @MockK
    lateinit var remoteReadGateway: ChapterReadGateway<ChapterRemoteInfoPageDto>

    @MockK
    lateinit var cacheHandler: ChapterCacheHandler

    private lateinit var useCase: ObserveCombinedChaptersUseCase

    private val inMemoryCache = mutableMapOf<String, ChapterDto>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        inMemoryCache.clear()

        every { localSyncStatusGateway.progress } returns MutableStateFlow(value = 0)
        every { localSyncStatusGateway.isIndexing } returns MutableStateFlow(value = false)

        every { cacheHandler.generateKey(any(), any(), any(), any(), any(), any(), any()) } answers {
            val args = it.invocation.args
            "${args[0]}_${args[1]}_${args[2]}_${args[3]}_${args[4]}_${args[5]}_${args[6]}"
        }
        every { cacheHandler.get(any()) } answers { inMemoryCache[it.invocation.args[0] as String] }
        every { cacheHandler.put(any(), any()) } answers {
            val key = it.invocation.args[0] as String
            val value = it.invocation.args[1] as ChapterDto
            if (value.archive.items.isNotEmpty() || value.archive.volumeSections.isNotEmpty()) {
                inMemoryCache[key] = value
            }
            Unit
        }

        useCase =
            ObserveCombinedChaptersUseCase(
                volumeGateway = volumeGateway,
                localReadGateway = localReadGateway,
                localSyncStatusGateway = localSyncStatusGateway,
                remoteReadGateway = remoteReadGateway,
                cacheHandler = cacheHandler,
            )
    }

    /**
     * Regressão: metadados não devem ser servidos do cache quando o remoteId muda de nulo para um valor válido.
     *
     * Cenário do bug: a primeira chamada com remoteId=null cacheava o resultado sem metadados.
     * A segunda chamada com remoteId=42L recebia o cache da primeira por usarem a mesma chave
     * (remoteId não fazia parte da chave), fazendo os metadados nunca chegarem na UI.
     */
    @Test
    fun `metadados de capitulo nao devem ser servidos do cache quando remoteId muda de nulo para valido`() =
        runTest {
            val localChapter =
                ChapterFileDto(
                    id = 1L,
                    name = "Ch 1",
                    path = "/ch1.cbz",
                    chapterSort = "1",
                )
            val remoteChapter =
                ChapterFeedDto(
                    id = 10L,
                    title = "Remote Title",
                    chapter = "1",
                    pageCount = 20,
                    scanlation = "Group",
                    source = emptyList(),
                )
            val localPage =
                ChapterPageDto(
                    items = listOf(localChapter),
                    pageSize = 20,
                    page = 0,
                    total = 1,
                )
            val remotePage =
                ChapterRemoteInfoPageDto(
                    items = listOf(remoteChapter),
                    pageSize = 20,
                    page = 0,
                    total = 1,
                )

            val sort = ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING)
            val localFlow = MutableStateFlow(localPage)
            val volumeGroupsFlow = MutableStateFlow<List<VolumeChapterGroupDto>>(emptyList())
            val remoteFlow = MutableStateFlow(remotePage)
            val hasRootFlow = MutableStateFlow(false)

            every { localReadGateway.observeChapters(1L, "NUMBER", true) } returns localFlow
            every { volumeGateway.observeVolumeGroups(1L, 20, "NUMBER", true) } returns volumeGroupsFlow
            every { volumeGateway.observeHasRootChapters(1L) } returns hasRootFlow
            every { remoteReadGateway.observeChapters(42L, "NUMBER", true) } returns remoteFlow

            // First call: remoteId=null → chapters load with dummy remote metadata and are cached
            val firstResult =
                useCase
                    .observeCombined(
                        comicId = 1L,
                        remoteId = null,
                        sort = sort,
                        page = 0,
                        pageSize = 20,
                        viewMode = VolumeViewType.CHAPTER,
                        volumeOverrides = emptyMap(),
                    ).first()

            assertNotNull(firstResult)
            assertEquals(1, firstResult!!.remoteInfo?.items?.size ?: 0)
            assertEquals(
                -1L,
                firstResult.remoteInfo
                    ?.items
                    ?.get(0)
                    ?.id,
            )

            // Second call: remoteId=42L → must NOT hit the cache from the first call
            // Bug: the same cache key was generated for both calls, serving empty metadata
            // Fix: remoteId must be part of the cache key so each call gets its own entry
            val secondResult =
                useCase
                    .observeCombined(
                        comicId = 1L,
                        remoteId = 42L,
                        sort = sort,
                        page = 0,
                        pageSize = 20,
                        viewMode = VolumeViewType.CHAPTER,
                        volumeOverrides = emptyMap(),
                    ).first()

            assertNotNull(secondResult)
            assertEquals(1, secondResult!!.remoteInfo!!.items.size)
            assertEquals(10L, secondResult.remoteInfo!!.items[0].id)
            assertEquals("Remote Title", secondResult.remoteInfo!!.items[0].title)
        }

    /**
     * Regressão: metadados devem ser atualizados quando o fluxo remoto emite novos dados,
     * mesmo que já exista uma entrada no cache (que pode estar obsoleta ou incompleta).
     */
    @Test
    fun `metadados devem ser atualizados quando o fluxo remoto emite novos dados mesmo com cache`() =
        runTest {
            val localChapter = ChapterFileDto(id = 1L, name = "Ch 1", path = "/ch1.cbz", chapterSort = "1")
            val localPage = ChapterPageDto(items = listOf(localChapter), pageSize = 20, page = 0, total = 1)

            val remoteChapter =
                ChapterFeedDto(id = 10L, title = "Remote Title", chapter = "1", pageCount = 20, scanlation = "Group", source = emptyList())
            val emptyRemotePage = ChapterRemoteInfoPageDto(items = emptyList(), pageSize = 20, page = 0, total = 0)
            val fullRemotePage = ChapterRemoteInfoPageDto(items = listOf(remoteChapter), pageSize = 20, page = 0, total = 1)

            val sort = ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING)
            val localFlow = MutableStateFlow(localPage)
            val volumeGroupsFlow = MutableStateFlow<List<VolumeChapterGroupDto>>(emptyList())
            val remoteFlow = MutableStateFlow(emptyRemotePage)
            val hasRootFlow = MutableStateFlow(false)

            every { localReadGateway.observeChapters(1L, "NUMBER", true) } returns localFlow
            every { volumeGateway.observeVolumeGroups(1L, 20, "NUMBER", true) } returns volumeGroupsFlow
            every { volumeGateway.observeHasRootChapters(1L) } returns hasRootFlow
            every { remoteReadGateway.observeChapters(42L, "NUMBER", true) } returns remoteFlow

            // 1. First emission with empty remote metadata
            val results = mutableListOf<ChapterDto?>()
            val job =
                launch {
                    useCase
                        .observeCombined(
                            comicId = 1L,
                            remoteId = 42L,
                            sort = sort,
                            page = 0,
                            pageSize = 20,
                            viewMode = VolumeViewType.CHAPTER,
                            volumeOverrides = emptyMap(),
                        ).collect { results.add(it) }
                }

            // Wait for first emission
            runCurrent()
            assertEquals(1, results.size)
            assertEquals(1, results[0]?.remoteInfo?.items?.size ?: 0)
            assertEquals(
                -1L,
                results[0]
                    ?.remoteInfo
                    ?.items
                    ?.get(0)
                    ?.id,
            )

            // 2. Simulate remote sync completion by emitting new data into remoteFlow
            remoteFlow.value = fullRemotePage
            runCurrent()

            // The combine should re-trigger and produce a new result with metadata
            // BUT if the cache bug exists, it will return the cached empty result!
            assertEquals(2, results.size)
            assertEquals(1, results[1]?.remoteInfo?.items?.size ?: 0)
            assertEquals(
                10L,
                results[1]
                    ?.remoteInfo
                    ?.items
                    ?.get(0)
                    ?.id,
            )
            assertEquals(
                "Remote Title",
                results[1]
                    ?.remoteInfo
                    ?.items
                    ?.first()
                    ?.title,
            )

            job.cancel()
        }
}
