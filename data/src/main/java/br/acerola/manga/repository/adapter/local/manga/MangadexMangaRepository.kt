package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.exception.IntegrityException
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.database.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.database.dao.metadata.source.MangadexSourceDao
import br.acerola.manga.local.database.entity.archive.MangaDirectory
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.local.mapper.toMangadexSource
import br.acerola.manga.local.mapper.toModel
import br.acerola.manga.repository.di.Mangadex
import br.acerola.manga.repository.port.AnilistLink
import br.acerola.manga.repository.port.AnilistLinkRepository
import br.acerola.manga.repository.port.BinaryOperationsRepository
import br.acerola.manga.repository.port.MangaManagementRepository
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.service.archive.MangaSaveCoverService
import br.acerola.manga.service.metadata.MangaMetadataExportService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class MangadexMangaRepository @Inject constructor(
    private val genreDao: GenreDao,
    private val authorDao: AuthorDao,
    private val directoryDao: MangaDirectoryDao,
    private val coverService: MangaSaveCoverService,
    private val mangadexSourceDao: MangadexSourceDao,
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
    @param:ApplicationContext private val context: Context,
    private val metadataExportService: MangaMetadataExportService,
    @param:Mangadex private val downloadCoverService: BinaryOperationsRepository<String>
) : MangaManagementRepository<MangaRemoteInfoDto>, AnilistLinkRepository {

    @Inject
    @Mangadex
    lateinit var mangadexChapterInfoService: RemoteInfoOperationsRepository<ChapterRemoteInfoDto, String>

    @Inject
    @Mangadex
    lateinit var mangadexMangaInfoService: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshManga(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Initiating MangaDex sync for manga: $mangaId", LogSource.REPOSITORY)
            _isIndexing.value = true
            try {
                Either.catch {
                    val directory = directoryDao.getMangaDirectoryById(mangaId) ?: return@catch
                    executeSync(folders = listOf(directory), baseUri = baseUri)
                }.mapLeft { exception ->
                    AcerolaLogger.e(TAG, "Refresh specific MangaDex metadata failed", LogSource.REPOSITORY, throwable = exception)
                    when (exception) {
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            } finally {
                _isIndexing.value = false
            }
        }

    override suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        AcerolaLogger.i(TAG, "Starting incremental MangaDex sync", LogSource.REPOSITORY)
        _isIndexing.value = true

        try {
            return withContext(context = Dispatchers.IO) {
                Either.catch {
                    val localDirectories = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                    val allRemoteMangaInfo = mangaRemoteInfoDao.getAllMangaRemoteInfo().firstOrNull() ?: emptyList()
                    val existingTitles = allRemoteMangaInfo.map { normalizeName(name = it.title) }.toSet()

                    val remoteInfoToSync = localDirectories.filter {
                        normalizeName(it.name) !in existingTitles
                    }

                    if (remoteInfoToSync.isEmpty()) {
                        AcerolaLogger.d(TAG, "No new mangas to sync with MangaDex", LogSource.REPOSITORY)
                        _progress.value = -1
                        return@catch
                    }

                    AcerolaLogger.d(TAG, "Found ${remoteInfoToSync.size} mangas needing metadata", LogSource.REPOSITORY)
                    executeSync(folders = remoteInfoToSync, baseUri)
                }.mapLeft { exception ->
                    AcerolaLogger.e(TAG, "Incremental MangaDex scan failed", LogSource.REPOSITORY, throwable = exception)
                    when (exception) {
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            }
        } finally {
            _isIndexing.value = false
        }
    }

    override suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        AcerolaLogger.i(TAG, "Starting full library MangaDex refresh", LogSource.REPOSITORY)
        _isIndexing.value = true

        try {
            return withContext(context = Dispatchers.IO) {
                Either.catch {
                    val mangaLibraryFolders = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()

                    if (mangaLibraryFolders.isEmpty()) {
                        _progress.value = -1
                        return@catch
                    }

                    executeSync(folders = mangaLibraryFolders, baseUri)
                }.mapLeft { exception ->
                    AcerolaLogger.e(TAG, "Full MangaDex refresh failed", LogSource.REPOSITORY, throwable = exception)
                    when (exception) {
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            }
        } finally {
            _isIndexing.value = false
        }
    }

    override suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        return refreshLibrary(baseUri)
    }

    override fun observeLibrary(): StateFlow<List<MangaRemoteInfoDto>> {
        return mangaRemoteInfoDao.getAllMangasWithRelations().map { remoteInfoRelations ->
            AcerolaLogger.d(TAG, "Observed MangaDex metadata update: ${remoteInfoRelations.size} entries", LogSource.REPOSITORY)

            coroutineScope {
                remoteInfoRelations.map { remoteInfo ->
                    async(context = Dispatchers.IO) {
                        remoteInfo.toDto()
                    }
                }.awaitAll()
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    private suspend fun executeSync(folders: List<MangaDirectory>, baseUri: Uri?) {
        val total = folders.size
        _progress.value = 0

        val rootPath = baseUri?.toString() ?: MangaDirectoryPreference.folderUriFlow(context).firstOrNull()
        if (rootPath.isNullOrBlank()) {
            AcerolaLogger.w(TAG, "Sync aborted: root library path is null", LogSource.REPOSITORY)
            _progress.value = -1
            return
        }

        val rootUri = rootPath.toUri()
        folders.forEachIndexed { index, current ->
            val result = Either.catch {
                val title = current.name
                val folderNameNormalized = normalizeName(name = title)

                val fetchedListResult = mangadexMangaInfoService.searchInfo(manga = title)
                val fetchedList = fetchedListResult.getOrNull() ?: emptyList()

                val bestMatch = fetchedList.find { candidate ->
                    normalizeName(name = candidate.title) == folderNameNormalized || normalizeName(name = candidate.romanji.orEmpty()) == folderNameNormalized
                } ?: fetchedList.firstOrNull()

                if (bestMatch != null) {
                    AcerolaLogger.v(TAG, "Found best match for '$title' -> '${bestMatch.title}'", LogSource.REPOSITORY)
                    val existingRemote = mangaRemoteInfoDao.getMangaByDirectoryId(current.id).firstOrNull()

                    val mangaToSave = bestMatch.toModel().copy(
                        id = existingRemote?.id ?: 0L,
                        mangaDirectoryFk = current.id
                    )

                    val mangaId = if (existingRemote != null) {
                        mangaRemoteInfoDao.update(mangaToSave)
                        existingRemote.id
                    } else {
                        mangaRemoteInfoDao.insert(mangaToSave)
                    }

                    if (mangaId != -1L) {
                        mangadexSourceDao.insert(bestMatch.toMangadexSource(mangaId))

                        bestMatch.authors?.let {
                            authorDao.insert(entity = it.toModel(mangaId = mangaId))
                        }

                        bestMatch.genre.forEach {
                            genreDao.insert(entity = it.toModel(mangaId = mangaId))
                        }

                        bestMatch.cover?.let { dto ->
                            downloadCoverService.searchCover(dto.url).onRight { bytes ->
                                coverService.processCover(
                                    rootUri = rootUri,
                                    folderId = current.id,
                                    bytes = bytes,
                                    coverUrl = dto.url,
                                    mangaFolderName = current.name,
                                    mangaRemoteInfoFk = mangaId
                                )
                            }
                        }

                        metadataExportService.exportMangaMetadata(directoryId = current.id, remoteInfo = bestMatch)
                    }
                } else {
                    AcerolaLogger.d(TAG, "No MangaDex match found for: $title", LogSource.REPOSITORY)
                }
            }

            result.onLeft { exception ->
                AcerolaLogger.e(TAG, "Error syncing metadata for ${current.name}", LogSource.REPOSITORY, throwable = exception)
                when (exception) {
                    is SQLiteException, is IntegrityException -> LibrarySyncError.DatabaseError(cause = exception)
                    is IOException -> LibrarySyncError.DiskIOFailure(path = current.path, cause = exception)
                    is MangadexRequestException -> LibrarySyncError.MangadexError(cause = exception)
                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                }
            }

            val currentProgress = (((index + 1).toFloat() / total.toFloat()) * 100).roundToInt()
            _progress.value = currentProgress
        }

        _progress.value = -1
    }

    override suspend fun getAnilistLink(directoryId: Long): Either<LibrarySyncError, AnilistLink> {
        val remoteInfo = mangaRemoteInfoDao.getMangaByDirectoryId(directoryId).firstOrNull()
            ?: return Either.Left(
                LibrarySyncError.UnexpectedError(cause = Exception("No remote info found for directory: $directoryId"))
            )

        val mangadexSource = mangadexSourceDao.getByMangaRemoteInfoFk(remoteInfo.id)
            ?: return Either.Left(
                LibrarySyncError.UnexpectedError(cause = Exception("No MangaDex source found for manga: ${remoteInfo.id}"))
            )

        val anilistId = mangadexSource.anilistId
            ?: return Either.Left(
                LibrarySyncError.UnexpectedError(cause = Exception("No AniList ID found for manga: ${remoteInfo.id}"))
            )

        return Either.Right(AnilistLink(anilistId = anilistId, remoteInfoId = remoteInfo.id))
    }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }

    companion object {
        private const val TAG = "MangadexMangaRepository"
    }
}
