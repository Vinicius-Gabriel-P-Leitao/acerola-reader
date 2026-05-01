package br.acerola.comic.adapter.metadata.anilist.engine

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.metadata.anilist.source.AnilistFetchBannerSource
import br.acerola.comic.adapter.metadata.anilist.source.AnilistFetchCoverSource
import br.acerola.comic.adapter.metadata.anilist.source.AnilistSearchByTitleSource
import br.acerola.comic.config.preference.ComicDirectoryPreference
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.dao.metadata.relationship.AuthorDao
import br.acerola.comic.local.dao.metadata.relationship.GenreDao
import br.acerola.comic.local.dao.metadata.source.AnilistSourceDao
import br.acerola.comic.local.translator.persistence.toAnilistSourceEntity
import br.acerola.comic.local.translator.persistence.toEntity
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.metadata.MetadataSource
import br.acerola.comic.service.artwork.BannerSaver
import br.acerola.comic.service.artwork.CoverSaver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistComicEngine
    @Inject
    constructor(
        private val genreDao: GenreDao,
        private val authorDao: AuthorDao,
        private val directoryDao: ComicDirectoryDao,
        private val comicMetadataDao: ComicMetadataDao,
        private val anilistSourceDao: AnilistSourceDao,
        private val coverService: CoverSaver,
        private val coverFetcher: AnilistFetchCoverSource,
        private val bannerService: BannerSaver,
        private val bannerFetcher: AnilistFetchBannerSource,
        private val anilistSearchByTitleSource: AnilistSearchByTitleSource,
        @param:ApplicationContext private val context: Context,
    ) : ComicGateway<ComicMetadataDto> {
        private val _progress = MutableStateFlow(value = -1)
        override val progress: StateFlow<Int> = _progress.asStateFlow()

        private val _isIndexing = MutableStateFlow(value = false)
        override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

        override suspend fun refreshManga(
            comicId: Long,
            baseUri: Uri?,
        ): Either<LibrarySyncError, Unit> =
            withContext(Dispatchers.IO) {
                AcerolaLogger.audit(TAG, "Initiating AniList sync for comic: $comicId", LogSource.REPOSITORY)
                _isIndexing.value = true
                try {
                    val directory =
                        directoryDao.getDirectoryById(comicId)
                            ?: return@withContext Either.Left(
                                LibrarySyncError.UnexpectedError(cause = Exception("Directory not found")),
                            )

                    if (!directory.externalSyncEnabled) {
                        return@withContext Either.Left(LibrarySyncError.ExternalSyncDisabled)
                    }

                    val normalizedDirName = normalizeName(directory.name)

                    anilistSearchByTitleSource
                        .searchInfo(comic = directory.name, limit = 10)
                        .mapLeft { networkError ->
                            LibrarySyncError.RemoteNetworkError(error = networkError)
                        }.flatMap { results ->
                            val dto =
                                results.find { candidate ->
                                    normalizeName(candidate.title) == normalizedDirName ||
                                        normalizeName(candidate.romanji.orEmpty()) == normalizedDirName
                                } ?: results.firstOrNull()
                                    ?: return@flatMap Either.Left(
                                        LibrarySyncError.MetadataNotFound(
                                            source = MetadataSource.ANILIST.source,
                                            identifier = directory.name,
                                        ),
                                    )

                            Either
                                .catch {
                                    val comicToSave =
                                        dto.toEntity().copy(
                                            comicDirectoryFk = comicId,
                                            syncSource = MetadataSource.ANILIST.source,
                                        )

                                    val remoteInfoId =
                                        comicMetadataDao.upsertComicWithRelationsTransaction(
                                            metadata = comicToSave,
                                            authors = dto.authors?.let { listOf(it.toEntity(comicId = 0L)) } ?: emptyList(),
                                            genres = dto.genre.map { it.toEntity(comicId = 0L) },
                                            anilistSource = dto.toAnilistSourceEntity(comicRemoteInfoFk = 0L),
                                            authorDao = authorDao,
                                            genreDao = genreDao,
                                            anilistDao = anilistSourceDao,
                                        )

                                    if (remoteInfoId != -1L) {
                                        persistAnilistData(
                                            comicId = comicId,
                                            remoteInfoId = remoteInfoId,
                                            dto = dto,
                                            baseUri = baseUri,
                                        )
                                        AcerolaLogger.audit(
                                            TAG,
                                            "Successfully synced AniList metadata",
                                            LogSource.REPOSITORY,
                                            mapOf(
                                                "comicId" to comicId.toString(),
                                                "anilistId" to (
                                                    dto.sources
                                                        ?.anilist
                                                        ?.anilistId
                                                        ?.toString() ?: ""
                                                ),
                                            ),
                                        )
                                    }
                                }.mapLeft { exception ->
                                    LibrarySyncError.UnexpectedError(cause = exception)
                                }
                        }
                } finally {
                    _isIndexing.value = false
                }
            }

        override suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> =
            withContext(Dispatchers.IO) {
                AcerolaLogger.audit(TAG, "Starting full library AniList refresh", LogSource.REPOSITORY)
                try {
                    val directories =
                        (directoryDao.getVisibleDirectories().firstOrNull() ?: emptyList())
                            .filter { it.externalSyncEnabled }

                    directories.forEachIndexed { index, directory ->
                        refreshManga(directory.id, baseUri)
                        _progress.value = ((index + 1) * 100 / directories.size.coerceAtLeast(1))
                    }

                    Either.Right(Unit)
                } catch (exception: Exception) {
                    Either.Left(LibrarySyncError.UnexpectedError(cause = exception))
                }
            }

        override suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> =
            withContext(Dispatchers.IO) {
                AcerolaLogger.audit(TAG, "Starting incremental AniList sync", LogSource.REPOSITORY)
                try {
                    val directories =
                        (directoryDao.getVisibleDirectories().firstOrNull() ?: emptyList())
                            .filter { it.externalSyncEnabled }

                    val toSync =
                        directories.filter { directory ->
                            val remoteInfo =
                                comicMetadataDao.observeComicByDirectoryId(directory.id).firstOrNull() ?: return@filter true

                            val anilistSource = anilistSourceDao.getByMetadataId(remoteInfo.id)
                            anilistSource == null
                        }

                    toSync.forEachIndexed { index, directory ->
                        refreshManga(directory.id, baseUri)
                        _progress.value = ((index + 1) * 100 / toSync.size.coerceAtLeast(1))
                    }
                    Either.Right(Unit)
                } catch (exception: Exception) {
                    Either.Left(LibrarySyncError.UnexpectedError(cause = exception))
                }
            }

        private fun normalizeName(name: String): String = name.filter { it.isLetterOrDigit() }.lowercase()

        private suspend fun persistAnilistData(
            comicId: Long,
            remoteInfoId: Long,
            dto: ComicMetadataDto,
            baseUri: Uri?,
        ) {
            val directory = directoryDao.getDirectoryById(comicId) ?: return

            val rootPath =
                baseUri?.toString()
                    ?: ComicDirectoryPreference.folderUriFlow(context).firstOrNull()
                    ?: return

            val rootUri = rootPath.toUri()

            dto.sources?.anilist?.coverImage?.let { url ->
                coverFetcher.searchMedia(url).onRight { bytes ->
                    coverService.processCover(
                        rootUri = rootUri,
                        folderId = directory.id,
                        bytes = bytes,
                        coverUrl = url,
                        comicFolderName = directory.name,
                        comicRemoteInfoFk = remoteInfoId,
                    )
                }
            }

            dto.sources?.anilist?.bannerImage?.let { url ->
                bannerFetcher.searchMedia(url).onRight { bytes ->
                    bannerService.processBanner(
                        rootUri = rootUri,
                        folderId = directory.id,
                        bytes = bytes,
                        bannerUrl = url,
                        comicFolderName = directory.name,
                        comicRemoteInfoFk = remoteInfoId,
                    )
                }
            }
        }

        companion object {
            private const val TAG = "AnilistComicEngine"
        }
    }
