package br.acerola.manga.adapter.metadata.mangadex.engine

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.adapter.contract.provider.ImageProvider
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.adapter.metadata.mangadex.MangadexSource
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.MangaMetadataDao
import br.acerola.manga.local.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.dao.metadata.source.MangadexSourceDao
import br.acerola.manga.local.entity.archive.MangaDirectory
import br.acerola.manga.local.translator.persistence.toEntity
import br.acerola.manga.local.translator.persistence.toMangadexSourceEntity
import br.acerola.manga.local.translator.ui.toViewDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.pattern.MetadataSourcePattern
import br.acerola.manga.service.artwork.CoverSaver
import br.acerola.manga.service.metadata.MetadataExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexMangaEngine @Inject constructor(
    private val genreDao: GenreDao,
    private val authorDao: AuthorDao,
    private val directoryDao: MangaDirectoryDao,
    private val coverService: CoverSaver,
    private val mangadexSourceDao: MangadexSourceDao,
    private val mangaMetadataDao: MangaMetadataDao,
    private val metadataExportService: MetadataExporter,
    @param:ApplicationContext private val context: Context,
    @param:MangadexSource private val downloadCoverService: ImageProvider<String>
) : MangaGateway<MangaMetadataDto> {



    @Inject
    @MangadexSource
    lateinit var mangadexSourceChapterInfoService: MetadataProvider<ChapterMetadataDto, String>

    @Inject
    @MangadexSource
    lateinit var mangadexSourceMangaInfoService: MetadataProvider<MangaMetadataDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshManga(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Initiating MangaDex sync for manga: $mangaId", LogSource.REPOSITORY)
            _isIndexing.value = true
            try {
                val directory = directoryDao.getMangaDirectoryById(mangaId)
                    ?: return@withContext Either.Left(LibrarySyncError.UnexpectedError(Exception("Directory not found")))

                if (!directory.externalSyncEnabled) {
                    return@withContext Either.Left(LibrarySyncError.ExternalSyncDisabled)
                }

                Either.catch {
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
                val existingRemote = mangaMetadataDao.getAllMangaRemoteInfo().firstOrNull() ?: emptyList()
                val existingDirectoryIds = existingRemote.mapNotNull { it.mangaDirectoryFk }.toSet()

                val foldersToSync = allFolders.filter { folder ->
                    !existingDirectoryIds.contains(folder.id) && folder.externalSyncEnabled
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
                    val allFolders = (directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList())
                        .filter { it.externalSyncEnabled }
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

    override fun observeLibrary(): Flow<List<MangaMetadataDto>> {
        return mangaMetadataDao.getAllMangasWithRelations().map { remoteInfoRelations ->
            AcerolaLogger.d(TAG, "Observed MangaDex metadata update: ${remoteInfoRelations.size} entries", LogSource.REPOSITORY)
            remoteInfoRelations.map { it.toViewDto() }
        }
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
            Either.catch {
                val title = current.name
                val folderNameNormalized = normalizeName(name = title)

                val fetchedListResult = mangadexSourceMangaInfoService.searchInfo(manga = title)
                val fetchedList = fetchedListResult.getOrNull() ?: emptyList()

                val bestMatch = fetchedList.find { candidate ->
                    normalizeName(name = candidate.title) == folderNameNormalized || normalizeName(name = candidate.romanji.orEmpty()) == folderNameNormalized
                } ?: fetchedList.firstOrNull()

                if (bestMatch != null) {
                    AcerolaLogger.v(TAG, "Found best match for '$title' -> '${bestMatch.title}'", LogSource.REPOSITORY)

                    val mangaToSave = bestMatch.toEntity().copy(
                        mangaDirectoryFk = current.id,
                        syncSource = MetadataSourcePattern.MANGADEX.source
                    )

                    val mangaId = mangaMetadataDao.upsertMangaMetadataTransaction(
                        metadata = mangaToSave,
                        authors = bestMatch.authors?.let { listOf(it.toEntity(mangaId = 0L)) } ?: emptyList(),
                        genres = bestMatch.genre.map { it.toEntity(mangaId = 0L) },
                        mangadexSource = bestMatch.toMangadexSourceEntity(mangaRemoteInfoFk = 0L),
                        authorDao = authorDao,
                        genreDao = genreDao,
                        mangadexDao = mangadexSourceDao
                    )

                    if (mangaId != -1L) {
                        bestMatch.cover?.let { dto ->
                            AcerolaLogger.d(TAG, "Syncing cover for ${current.name}", LogSource.REPOSITORY)
                            downloadCoverService.searchMedia(dto.url).onRight { bytes ->
                                coverService.processCover(
                                    rootUri = rootUri,
                                    folderId = current.id,
                                    bytes = bytes,
                                    coverUrl = dto.url,
                                    mangaFolderName = current.name,
                                    mangaRemoteInfoFk = mangaId
                                )
                            }.onLeft {
                                AcerolaLogger.e(TAG, "Failed to download cover for ${current.name}", LogSource.REPOSITORY)
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
