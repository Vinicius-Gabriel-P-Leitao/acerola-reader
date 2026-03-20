package br.acerola.manga.adapter.metadata.mangadex.engine

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.manga.adapter.contract.ImageFetchPort
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.adapter.contract.RemoteInfoOperationsPort
import br.acerola.manga.adapter.metadata.mangadex.MangadexSource
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.exception.IntegrityException
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.dao.metadata.source.MangadexSourceDao
import br.acerola.manga.local.entity.archive.MangaDirectory
import br.acerola.manga.local.entity.relation.RemoteInfoRelations
import br.acerola.manga.local.translator.toDto
import br.acerola.manga.local.translator.toMangadexSource
import br.acerola.manga.local.translator.toModel
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.service.artwork.MangaSaveCoverService
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
class MangadexMangaEngine @Inject constructor(
    private val genreDao: GenreDao,
    private val authorDao: AuthorDao,
    private val directoryDao: MangaDirectoryDao,
    private val coverService: MangaSaveCoverService,
    private val mangadexSourceDao: MangadexSourceDao,
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
    @param:ApplicationContext private val context: Context,
    private val metadataExportService: MangaMetadataExportService,
    @param:MangadexSource private val downloadCoverService: ImageFetchPort<String>
) : MangaPort<MangaRemoteInfoDto> {

    @Inject
    @MangadexSource
    lateinit var mangadexSourceChapterInfoService: RemoteInfoOperationsPort<ChapterRemoteInfoDto, String>

    @Inject
    @MangadexSource
    lateinit var mangadexSourceMangaInfoService: RemoteInfoOperationsPort<MangaRemoteInfoDto, String>

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
        return withContext(context = Dispatchers.IO) {
            try {
                val allFolders = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                val existingRemote = mangaRemoteInfoDao.getAllMangaRemoteInfo().firstOrNull() ?: emptyList()
                val existingDirectoryIds = existingRemote.mapNotNull { it.mangaDirectoryFk }.toSet()

                val foldersToSync = allFolders.filter { folder ->
                    !existingDirectoryIds.contains(folder.id)
                }

                executeSync(folders = foldersToSync, baseUri = baseUri)
                Either.Right(value = Unit)
            } catch (exception: Exception) {
                AcerolaLogger.e(TAG, "Incremental MangaDex scan failed", LogSource.REPOSITORY, throwable = exception)
                when (exception) {
                    is SQLiteException -> Either.Left(LibrarySyncError.DatabaseError(cause = exception))
                    else -> Either.Left(LibrarySyncError.UnexpectedError(cause = exception))
                }
            } finally {
                _isIndexing.value = false
            }
        }
    }

    override suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        AcerolaLogger.i(TAG, "Starting full library MangaDex refresh", LogSource.REPOSITORY)
        _isIndexing.value = true
        return try {
            withContext(context = Dispatchers.IO) {
                Either.catch {
                    val allFolders = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                    executeSync(folders = allFolders, baseUri = baseUri)
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

                val fetchedListResult = mangadexSourceMangaInfoService.searchInfo(manga = title)
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

                        AcerolaLogger.audit(
                            TAG, "Successfully synced MangaDex metadata", LogSource.REPOSITORY,
                            mapOf("mangaId" to current.id.toString(), "mangadexId" to (bestMatch.sources?.mangadex?.mangadexId ?: ""))
                        )

                        metadataExportService.exportMangaMetadata(directoryId = current.id, remoteInfo = bestMatch)
                    }
                } else {
                    AcerolaLogger.d(TAG, "No MangaDex match found for: $title", LogSource.REPOSITORY)
                }
            }

            _progress.value = ((index + 1) * 100 / total)
        }

        _progress.value = -1
    }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }

    companion object {
        private const val TAG = "MangadexMangaRepository"
    }
}
