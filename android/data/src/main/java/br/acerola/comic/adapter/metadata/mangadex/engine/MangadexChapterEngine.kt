package br.acerola.comic.adapter.metadata.mangadex.engine

import android.database.sqlite.SQLiteException
import android.net.Uri
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.adapter.metadata.mangadex.MangadexSource
import br.acerola.comic.data.R
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.error.exception.MangadexRequestException
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.comic.local.dao.metadata.ChapterMetadataDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.relation.ChapterVolumeJoin
import br.acerola.comic.local.translator.persistence.toDownloadSourcesEntities
import br.acerola.comic.local.translator.persistence.toEntity
import br.acerola.comic.local.translator.ui.toViewDto
import br.acerola.comic.local.translator.ui.toViewPageDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.service.metadata.MetadataExporter
import br.acerola.comic.util.normalizeChapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexChapterEngine
    @Inject
    constructor(
        private val directoryDao: ComicDirectoryDao,
        private val chapterArchiveDao: ChapterArchiveDao,
        private val comicMetadataDao: ComicMetadataDao,
        private val chapterMetadataDao: ChapterMetadataDao,
        private val metadataExportService: MetadataExporter,
        private val chapterDownloadSourceDao: ChapterDownloadSourceDao,
    ) : ChapterGateway<ChapterRemoteInfoPageDto> {
        @Inject
        @MangadexSource
        lateinit var mangadexSourceChapterInfoService: MetadataProvider<ChapterMetadataDto, String>

        private val _progress = MutableStateFlow(value = -1)
        override val progress: StateFlow<Int> = _progress.asStateFlow()

        private val _isIndexing = MutableStateFlow(value = false)
        override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

        override suspend fun refreshComicChapters(
            comicId: Long,
            baseUri: Uri?,
        ): Either<LibrarySyncError, Unit> =
            withContext(context = Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Starting MangaDex chapter metadata sync for comic: $comicId", LogSource.REPOSITORY)
                _isIndexing.value = true
                _progress.value = 0

                val remoteMangaRelations =
                    try {
                        comicMetadataDao.observeComicWithRelationsByDirectoryId(comicId).first()
                    } catch (exception: Exception) {
                        AcerolaLogger.e(TAG, "Database error while fetching comic relations", LogSource.REPOSITORY, throwable = exception)
                        _isIndexing.value = false
                        return@withContext Either.Left(value = LibrarySyncError.DatabaseError(cause = exception))
                    }

                if (remoteMangaRelations == null) {
                    AcerolaLogger.w(TAG, "Sync aborted: No remote info link for comic $comicId", LogSource.REPOSITORY)
                    _isIndexing.value = false
                    return@withContext Either.Right(value = Unit)
                }

                val remoteManga = remoteMangaRelations.remoteInfo
                val mangadexId =
                    remoteMangaRelations.mangadexSource?.mangadexId ?: run {
                        AcerolaLogger.w(TAG, "Sync aborted: No MangaDex source for comic ${remoteManga.id}", LogSource.REPOSITORY)
                        _isIndexing.value = false
                        return@withContext Either.Right(value = Unit)
                    }

                mangadexSourceChapterInfoService
                    .searchInfo(comic = mangadexId, limit = 100, onProgress = {
                        _progress.value = it
                    })
                    .mapLeft {
                        AcerolaLogger.e(TAG, "MangaDex API request failed for mangadexId: $mangadexId", LogSource.REPOSITORY)
                        LibrarySyncError.SyncNetworkError(cause = null)
                    }.flatMap { remoteChapters ->
                        AcerolaLogger.d(TAG, "Fetched ${remoteChapters.size} chapters from MangaDex", LogSource.REPOSITORY)
                        _progress.value = 90
                        Either
                            .catch {
                                val localDirectory =
                                    directoryDao.getDirectoryById(comicId)
                                        ?: throw Exception("Local directory not found for ID: $comicId")

                                val localChapters = chapterArchiveDao.getChaptersByDirectoryId(localDirectory.id).first()

                                val chapterPairs = matchRemoteWithArchive(remote = remoteChapters, local = localChapters)
                                AcerolaLogger.d(
                                    TAG,
                                    "Matched ${chapterPairs.size} chapters out of ${localChapters.size} local files",
                                    LogSource.REPOSITORY,
                                )

                                if (chapterPairs.isEmpty()) {
                                    throw MangadexRequestException(
                                        title = R.string.title_remote_info_null_error,
                                        description = R.string.description_remote_info_null_error,
                                    )
                                }

                                chapterPairs.forEach { (archive, remote) ->
                                    val chapterRemoteInfoEntity = remote.toEntity(comicRemoteInfoFk = remoteManga.id)
                                    val chapterRemoteInfoId = chapterMetadataDao.insert(chapterRemoteInfoEntity)

                                    val downloadSourceEntities = remote.toDownloadSourcesEntities(chapterFk = chapterRemoteInfoId)
                                    chapterDownloadSourceDao.insertAll(*downloadSourceEntities.toTypedArray())
                                }

                                metadataExportService.exportFull(
                                    directoryId = comicId,
                                    comicInfo = remoteMangaRelations.toViewDto(),
                                )

                                AcerolaLogger.i(TAG, "MangaDex chapter sync completed for: ${localDirectory.name}", LogSource.REPOSITORY)
                                _progress.value = 100
                            }.mapLeft { exception ->
                                AcerolaLogger.e(TAG, "Error matching/saving MangaDex chapters", LogSource.REPOSITORY, throwable = exception)
                                when (exception) {
                                    is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                                    is MangadexRequestException -> LibrarySyncError.MangadexError(cause = exception)
                                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                                }
                            }
                    }.also {
                        _isIndexing.value = false
                        _progress.value = -1
                    }
            }

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun observeChapters(
            comicId: Long,
            sortType: String,
            isAscending: Boolean,
        ): StateFlow<ChapterRemoteInfoPageDto> =
            chapterMetadataDao
                .observeChaptersByMetadataId(comicId)
                .flatMapLatest { chapters ->
                    val chapterIds = chapters.map { it.id }

                    flow {
                        val sources =
                            chapterIds
                                .takeIf { it.isNotEmpty() }
                                ?.let {
                                    chapterDownloadSourceDao.observeChapterDownloadSourcesByChapterIds(it).first()
                                }.orEmpty()

                        // Add simple in-memory sort for remote metadata to match local
                        val baseList =
                            if (sortType == "LAST_UPDATE") {
                                chapters.sortedBy { it.id }
                            } else {
                                // Default NUMBER sorting for metadata
                                chapters.sortedBy { it.chapter.toDoubleOrNull() ?: 0.0 }
                            }

                        val finalList = if (isAscending) baseList else baseList.reversed()
                        emit(value = finalList.toViewPageDto(sources = sources))
                    }
                }.stateIn(
                    started = SharingStarted.Lazily,
                    scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
                    initialValue = ChapterRemoteInfoPageDto(items = emptyList(), pageSize = 0, total = 0, page = 0),
                )

        override suspend fun getChapterPage(
            comicId: Long,
            total: Int,
            page: Int,
            pageSize: Int,
            sortType: String,
            isAscending: Boolean,
        ): ChapterRemoteInfoPageDto {
            val offset = page * pageSize

            val realTotal =
                if (total != 0) {
                    total
                } else {
                    chapterMetadataDao.countChaptersByMetadataId(comicId)
                }

            // Para informações remotas, se for algo além de simples DESC/ASC em id/número, buscamos tudo e classificamos.
            // Mas geralmente as sincronizações de metadados são pequenas o suficiente para serem gerenciadas.
            // Seguiremos o mesmo padrão local: NUMBER ASC usa SQL, outros usam memória.
            val chapters =
                if (sortType == "NUMBER" && isAscending) {
                    chapterMetadataDao.getChaptersByMetadataPaged(comicId, pageSize, offset)
                } else {
                    val all = chapterMetadataDao.getChaptersListByMetadataId(comicId)
                    val base = if (sortType == "LAST_UPDATE") all.sortedBy { it.id } else all.sortedBy { it.chapter.toDoubleOrNull() ?: 0.0 }
                    val sorted = if (isAscending) base else base.reversed()

                    val start = offset.coerceIn(0, sorted.size)
                    val end = (start + pageSize).coerceIn(0, sorted.size)

                    if (start < sorted.size) sorted.subList(start, end) else emptyList()
                }

            val sources =
                chapters
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.id }
                    ?.let {
                        chapterDownloadSourceDao.observeChapterDownloadSourcesByChapterIds(it).first()
                    }.orEmpty()

            return chapters.toViewPageDto(
                sources = sources,
                pageSize = pageSize,
                total = realTotal,
                page = page,
            )
        }

        private fun matchRemoteWithArchive(
            remote: List<ChapterMetadataDto>,
            local: List<ChapterVolumeJoin>,
        ): List<Pair<ChapterArchive, ChapterMetadataDto>> {
            val remoteByChapter =
                remote
                    .mapNotNull { dto ->
                        val key = dto.chapter?.normalizeChapter()
                        if (key == null) null else key to dto
                    }.groupBy(keySelector = { it.first }, valueTransform = { it.second })
                    .mapValues { (_, list) ->
                        list.maxBy { it.mangadexVersion }
                    }

            return local.mapNotNull { join ->
                val archive = join.chapter
                val chapterKey = archive.chapterSort.normalizeChapter()
                val remoteInfo = remoteByChapter[chapterKey] ?: return@mapNotNull null

                archive to remoteInfo
            }
        }

        companion object {
            private const val TAG = "MangadexChapterRepository"
        }
    }
