package br.acerola.manga.engine.adapter.mangadex

import android.database.sqlite.SQLiteException
import android.net.Uri
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.data.R
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.mapper.toDownloadSources
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.local.mapper.toModel
import br.acerola.manga.local.mapper.toPageDto
import br.acerola.manga.source.di.Mangadex
import br.acerola.manga.engine.port.ChapterPort
import br.acerola.manga.source.port.RemoteInfoOperationsPort
import br.acerola.manga.service.metadata.MangaMetadataExportService
import br.acerola.manga.util.normalizeChapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
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
class MangadexChapterAdapter @Inject constructor(
    private val directoryDao: MangaDirectoryDao,
    private val chapterArchiveDao: ChapterArchiveDao,
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
    private val chapterRemoteInfoDao: ChapterRemoteInfoDao,
    private val metadataExportService: MangaMetadataExportService,
    private val chapterDownloadSourceDao: ChapterDownloadSourceDao,
) : ChapterPort<ChapterRemoteInfoPageDto> {

    @Inject
    @Mangadex
    lateinit var mangadexChapterInfoService: RemoteInfoOperationsPort<ChapterRemoteInfoDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshMangaChapters(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Starting MangaDex chapter metadata sync for manga: $mangaId", LogSource.REPOSITORY)  
            _isIndexing.value = true
            _progress.value = 0

            val remoteMangaRelations = try {
                mangaRemoteInfoDao.getMangaWithRelationsByDirectoryId(mangaId).first()
            } catch (exception: Exception) {
                AcerolaLogger.e(TAG, "Database error while fetching manga relations", LogSource.REPOSITORY, throwable = exception)  
                _isIndexing.value = false
                return@withContext Either.Left(value = LibrarySyncError.DatabaseError(cause = exception))
            }

            if (remoteMangaRelations == null) {
                AcerolaLogger.w(TAG, "Sync aborted: No remote info link for manga $mangaId", LogSource.REPOSITORY)  
                _isIndexing.value = false
                return@withContext Either.Right(value = Unit)
            }

            val remoteManga = remoteMangaRelations.remoteInfo
            val mangadexId = remoteMangaRelations.mangadexSource?.mangadexId ?: run {
                AcerolaLogger.w(TAG, "Sync aborted: No MangaDex source for manga ${remoteManga.id}", LogSource.REPOSITORY)
                _isIndexing.value = false
                return@withContext Either.Right(value = Unit)
            }

            mangadexChapterInfoService.searchInfo(manga = mangadexId, limit = 100, onProgress = {
                _progress.value = it
            }).mapLeft {
                AcerolaLogger.e(TAG, "MangaDex API request failed for mangadexId: $mangadexId", LogSource.REPOSITORY)
                LibrarySyncError.NetworkError(cause = null)
            }.flatMap { remoteChapters ->
                AcerolaLogger.d(TAG, "Fetched ${remoteChapters.size} chapters from MangaDex", LogSource.REPOSITORY)  
                _progress.value = 90
                Either.catch {
                    val localDirectory = directoryDao.getMangaDirectoryById(mangaId)
                        ?: throw Exception("Local directory not found for ID: $mangaId")

                    val localChapters = chapterArchiveDao.getChaptersByMangaDirectory(localDirectory.id).first()

                    val chapterPairs = matchRemoteWithArchive(remote = remoteChapters, local = localChapters)
                    AcerolaLogger.d(TAG, "Matched ${chapterPairs.size} chapters out of ${localChapters.size} local files", LogSource.REPOSITORY)  

                    if (chapterPairs.isEmpty()) {
                        throw MangadexRequestException(
                            title = R.string.title_remote_info_null_error,
                            description = R.string.description_remote_info_null_error
                        )
                    }

                    chapterPairs.forEach { (archive, remote) ->
                        val chapterRemoteInfoEntity = remote.toModel(mangaRemoteInfoFk = remoteManga.id)
                        val chapterRemoteInfoId = chapterRemoteInfoDao.insert(chapterRemoteInfoEntity)

                        val downloadSourceEntities = remote.toDownloadSources(chapterFk = chapterRemoteInfoId)
                        chapterDownloadSourceDao.insertAll(*downloadSourceEntities.toTypedArray())
                    }

                    metadataExportService.exportFull(
                        directoryId = mangaId, mangaInfo = remoteMangaRelations.toDto()
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
    override fun observeChapters(mangaId: Long): StateFlow<ChapterRemoteInfoPageDto> {
        return chapterRemoteInfoDao.getChaptersByMangaRemoteInfo(mangaId).flatMapLatest { chapters ->
            val chapterIds = chapters.map { it.id }

            flow {
                val sources = chapterIds.takeIf { it.isNotEmpty() }?.let {
                    chapterDownloadSourceDao.getChapterDownloadSourceByRemoteInfoId(it).first()
                }.orEmpty()

                emit(value = chapters.toPageDto(sources = sources))
            }
        }.stateIn(
            started = SharingStarted.Lazily,
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            initialValue = ChapterRemoteInfoPageDto(items = emptyList(), pageSize = 0, total = 0, page = 0)
        )
    }

    override suspend fun getChapterPage(
        mangaId: Long, total: Int, page: Int, pageSize: Int
    ): ChapterRemoteInfoPageDto {
        val offset = page * pageSize

        val realTotal = if (total != 0) total
        else chapterRemoteInfoDao.countChaptersByMangaRemoteInfo(mangaId)

        val chapters = chapterRemoteInfoDao.getChaptersPaged(mangaId, pageSize, offset)

        val sources = chapters.takeIf { it.isNotEmpty() }?.map { it.id }?.let {
            chapterDownloadSourceDao.getChapterDownloadSourceByRemoteInfoId(it).first()
        }.orEmpty()

        return chapters.toPageDto(
            sources = sources, pageSize = pageSize, total = realTotal, page = page
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSpecificChapters(mangaId: Long, chapters: List<String>): Flow<ChapterRemoteInfoPageDto> {
        return chapterRemoteInfoDao.getChaptersByMangaAndNumbers(mangaId, chapters).flatMapLatest { chapterList ->
            val chapterIds = chapterList.map { it.id }

            flow {
                val sources: List<ChapterDownloadSource> = chapterIds.takeIf { it.isNotEmpty() }?.let {
                    chapterDownloadSourceDao.getChapterDownloadSourceByRemoteInfoId(it).first()
                } ?: emptyList()

                emit(value = chapterList.toPageDto(sources = sources))
            }
        }
    }

    private fun matchRemoteWithArchive(
        remote: List<ChapterRemoteInfoDto>, local: List<ChapterArchive>
    ): List<Pair<ChapterArchive, ChapterRemoteInfoDto>> {
        val remoteByChapter = remote.mapNotNull { dto ->
            val key = dto.chapter?.normalizeChapter()

            if (key == null) null else key to dto
        }.groupBy(keySelector = { it.first }, valueTransform = { it.second }).mapValues { (_, list) ->
            list.maxBy { it.mangadexVersion }
        }

        return local.mapNotNull { archive ->
            val chapterKey = archive.chapterSort.normalizeChapter()
            val remoteInfo = remoteByChapter[chapterKey] ?: return@mapNotNull null

            archive to remoteInfo
        }
    }

    companion object {
        private const val TAG = "MangadexChapterRepository"  
    }
}
